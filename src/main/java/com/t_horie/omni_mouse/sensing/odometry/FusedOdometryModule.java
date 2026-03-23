package com.t_horie.omni_mouse.sensing.odometry;

import com.t_horie.omni_mouse.hardware.imu.IMUData;
import com.t_horie.omni_mouse.hardware.imu.IMUModule;
import com.t_horie.omni_mouse.hardware.motor.MotorEncoderModule;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Odometry module that fuses wheel encoder and IMU data.
 *
 * <h2>Sensor roles</h2>
 * <ul>
 *   <li><b>Heading θ</b> — BNO055 NDOF absolute heading (no long-term drift)</li>
 *   <li><b>Position (x, y)</b> — encoder forward kinematics (accumulates Δrev → Δpose)</li>
 *   <li><b>ω</b> — IMU gyroscope Z-axis</li>
 *   <li><b>Vx, Vy</b> — encoder displacement / dt (body frame)</li>
 * </ul>
 *
 * <h2>Forward kinematics (3 wheels, 120° equal spacing)</h2>
 * <p>The inverse-kinematics matrix H satisfies H<sup>T</sup>H = diag(3/2, 3/2, 3r²),
 * so the pseudoinverse is:
 * <pre>
 *   Δx_body = (2/3) Σᵢ [−sin(φᵢ) · Δwᵢ]
 *   Δy_body = (2/3) Σᵢ [ cos(φᵢ) · Δwᵢ]
 * </pre>
 * where {@code Δwᵢ = Δrev[i] × 2π × r_wheel} (wheel contact displacement, metres).
 *
 * <h2>Coordinate convention</h2>
 * <ul>
 *   <li>+x = robot forward, +y = robot left (right-hand, z up)</li>
 *   <li>θ positive = counterclockwise when viewed from above</li>
 *   <li>BNO055 heading increases clockwise → negated on read</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * {@link #update()} and {@link #reset()} are {@code synchronized}.
 * {@link #getCurrentOdometry()} is lock-free via {@link AtomicReference}.
 */
public class FusedOdometryModule implements OdometryModule {

    private static final double SAMPLE_RATE_HZ = 100.0;

    // Wheel geometry — must stay in sync with OmniMotionModule
    private static final double CENTER_RADIUS_M   = 0.050; // 50 mm
    private static final double WHEEL_RADIUS_M    = 0.024; // 48 mm ÷ 2
    private static final double[] WHEEL_ANGLES_DEG = {0.0, 120.0, 240.0};

    private static final double DT_MAX_S = 0.5; // clamp for large gaps (e.g. first tick)

    private final IMUModule          imu;
    private final MotorEncoderModule encoder;

    // Pre-computed sin/cos of wheel placement angles
    private final double[] sinPhi = new double[3];
    private final double[] cosPhi = new double[3];

    // Odometry state — guarded by 'this'
    private double   x;
    private double   y;
    private double   thetaOffsetDeg; // IMU heading at last reset (degrees)
    private double[] lastRevolutions;
    private long     lastUpdateNs;

    private final AtomicReference<OdometryData> currentOdometry;

    /**
     * @param imu     BNO055 (or any IMU) providing absolute heading and gyro data
     * @param encoder L6470 (or any encoder) providing absolute wheel positions
     */
    public FusedOdometryModule(IMUModule imu, MotorEncoderModule encoder) {
        this.imu     = imu;
        this.encoder = encoder;

        for (int i = 0; i < 3; i++) {
            double rad = Math.toRadians(WHEEL_ANGLES_DEG[i]);
            sinPhi[i] = Math.sin(rad);
            cosPhi[i] = Math.cos(rad);
        }

        // Capture initial sensor state as reset reference
        thetaOffsetDeg = imu.readData().orientation().heading();
        lastRevolutions = encoder.getAbsolutePositions();
        lastUpdateNs    = System.nanoTime();

        currentOdometry = new AtomicReference<>(
                new OdometryData(Pose.zero(), Velocity.zero(), lastUpdateNs));
    }

    // -------------------------------------------------------------------------
    // OdometryModule interface
    // -------------------------------------------------------------------------

    @Override
    public Flux<OdometryData> start() {
        return Flux.interval(Duration.ofMillis((long) (1000.0 / SAMPLE_RATE_HZ)))
                .map(_ -> update());
    }

    @Override
    public synchronized void reset() {
        x = 0;
        y = 0;
        thetaOffsetDeg = imu.readData().orientation().heading();
        lastRevolutions = encoder.getAbsolutePositions();
        lastUpdateNs    = System.nanoTime();
        currentOdometry.set(new OdometryData(Pose.zero(), Velocity.zero(), lastUpdateNs));
    }

    @Override
    public OdometryData getCurrentOdometry() {
        return currentOdometry.get();
    }

    @Override
    public void shutdown() {
        // Encoder and IMU lifecycles are managed by their owners
    }

    // -------------------------------------------------------------------------
    // Core update
    // -------------------------------------------------------------------------

    private synchronized OdometryData update() {
        long   now = System.nanoTime();
        double dt  = Math.min((now - lastUpdateNs) * 1e-9, DT_MAX_S);
        if (dt <= 0) dt = 1.0 / SAMPLE_RATE_HZ;
        lastUpdateNs = now;

        // --- Encoder: wheel displacement delta ---
        double[] revolutions = encoder.getAbsolutePositions();
        double sumNegSin = 0, sumCos = 0;
        for (int i = 0; i < 3; i++) {
            // Δwᵢ = Δrev[i] × 2π × r_wheel  (metres at wheel contact)
            double dw = (revolutions[i] - lastRevolutions[i]) * 2.0 * Math.PI * WHEEL_RADIUS_M;
            sumNegSin += -sinPhi[i] * dw;
            sumCos    +=  cosPhi[i] * dw;
        }
        lastRevolutions = revolutions;

        // Forward kinematics → robot body-frame displacement
        double dx_body = (2.0 / 3.0) * sumNegSin;
        double dy_body = (2.0 / 3.0) * sumCos;

        // --- IMU: absolute heading for world-frame orientation ---
        // BNO055 heading is 0–360° clockwise → negate for CCW convention
        IMUData imuData    = imu.readData();
        double  headingDeg = imuData.orientation().heading();

        // Normalize heading relative to reset reference to [-180, +180]
        double relDeg = headingDeg - thetaOffsetDeg;
        relDeg = ((relDeg + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        double theta = -Math.toRadians(relDeg); // CCW positive

        // --- Transform body displacement → world frame ---
        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);
        x += dx_body * cosT - dy_body * sinT;
        y += dx_body * sinT + dy_body * cosT;

        // --- Velocity (body frame, per-axis) ---
        double vx    = dx_body / dt;
        double vy    = dy_body / dt;
        // Angular velocity: IMU gyro Z (deg/s → rad/s, negated for CCW convention)
        double omega = -Math.toRadians(imuData.angularVelocity().z());

        Pose         pose  = new Pose(x, y, theta).normalize();
        Velocity     vel   = new Velocity(vx, vy, omega);
        OdometryData odom  = new OdometryData(pose, vel, now);
        currentOdometry.set(odom);
        return odom;
    }
}
