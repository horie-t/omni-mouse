package com.t_horie.omni_mouse.sensing.odometry;

import com.t_horie.omni_mouse.hardware.imu.IMUData;
import com.t_horie.omni_mouse.hardware.imu.IMUModule;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * IMU-based odometry implementation.
 * Estimates position by integrating acceleration and uses IMU heading for orientation.
 *
 * Note: This is a basic implementation. For production use, consider:
 * - Complementary filter or Kalman filter for sensor fusion
 * - Drift compensation
 * - Integration with wheel encoders
 */
public class IMUOdometryModule implements OdometryModule {
    private static final double SAMPLE_RATE_HZ = 100.0;
    private static final double DT = 1.0 / SAMPLE_RATE_HZ;
    private static final double GRAVITY = 9.80665; // m/s²

    private final IMUModule imuModule;
    private final AtomicReference<OdometryData> currentOdometry;
    private final AtomicReference<VelocityState> velocityState;

    // Calibration offset for removing gravity component
    private double accelZOffset = GRAVITY;

    public IMUOdometryModule(IMUModule imuModule) {
        this.imuModule = imuModule;
        this.currentOdometry = new AtomicReference<>(
                new OdometryData(Pose.zero(), Velocity.zero(), System.nanoTime())
        );
        this.velocityState = new AtomicReference<>(new VelocityState(0.0, 0.0));
    }

    @Override
    public Flux<OdometryData> start() {
        return Flux.interval(Duration.ofMillis((long) (1000 / SAMPLE_RATE_HZ)))
                .map(_ -> updateOdometry());
    }

    private OdometryData updateOdometry() {
        IMUData imuData = imuModule.readData();
        long currentTimeNanos = System.nanoTime();
        OdometryData previousOdom = currentOdometry.get();
        VelocityState prevVelState = velocityState.get();

        // Extract orientation (theta) from IMU heading
        // Convert from degrees to radians
        double theta = Math.toRadians(imuData.orientation().heading());

        // Extract angular velocity (omega) from gyro Z-axis
        // Convert from deg/s to rad/s
        double omega = Math.toRadians(imuData.angularVelocity().z());

        // Extract linear acceleration in body frame
        // Note: BNO055 may already compensate for gravity in fusion mode
        // Adjust accelZ by removing gravity component
        double accelXBody = imuData.acceleration().x();
        double accelYBody = imuData.acceleration().y();
        double accelZBody = imuData.acceleration().z() - accelZOffset;

        // Transform acceleration from body frame to world frame
        double accelXWorld = accelXBody * Math.cos(theta) - accelYBody * Math.sin(theta);
        double accelYWorld = accelXBody * Math.sin(theta) + accelYBody * Math.cos(theta);

        // Integrate acceleration to get velocity (simple Euler integration)
        double vx = prevVelState.vx + accelXWorld * DT;
        double vy = prevVelState.vy + accelYWorld * DT;

        // Apply velocity decay to compensate for drift
        // (assuming friction/motor resistance causes velocity to decay without input)
        double decayFactor = 0.95;
        vx *= decayFactor;
        vy *= decayFactor;

        // Integrate velocity to get position
        double x = previousOdom.pose().x() + vx * DT;
        double y = previousOdom.pose().y() + vy * DT;

        // Update state
        velocityState.set(new VelocityState(vx, vy));

        Pose newPose = new Pose(x, y, theta).normalize();
        Velocity newVelocity = new Velocity(vx, vy, omega);
        OdometryData newOdometry = new OdometryData(newPose, newVelocity, currentTimeNanos);

        currentOdometry.set(newOdometry);
        return newOdometry;
    }

    @Override
    public void reset() {
        currentOdometry.set(new OdometryData(Pose.zero(), Velocity.zero(), System.nanoTime()));
        velocityState.set(new VelocityState(0.0, 0.0));
    }

    @Override
    public OdometryData getCurrentOdometry() {
        return currentOdometry.get();
    }

    @Override
    public void shutdown() {
        // Nothing to clean up
    }

    /**
     * Internal state for velocity tracking.
     */
    private record VelocityState(double vx, double vy) {}
}
