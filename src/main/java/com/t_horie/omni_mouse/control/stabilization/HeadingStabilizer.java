package com.t_horie.omni_mouse.control.stabilization;

import com.t_horie.omni_mouse.control.motion.MotionControlModule;
import com.t_horie.omni_mouse.hardware.imu.IMUModule;

/**
 * PID heading-hold stabilizer for the omnidirectional robot.
 *
 * <h2>Strategy</h2>
 * <pre>
 *   |ω_cmd| > deadband  →  intentional rotation
 *                           pass ω through unchanged
 *                           update target heading to current heading
 *
 *   |ω_cmd| ≤ deadband  →  straight-line / stationary mode
 *                           compute PID correction on heading error
 *                           forward (ω_cmd + Δω) to wrapped MotionControlModule
 * </pre>
 *
 * <h2>Heading convention</h2>
 * Uses the IMU's raw heading (BNO055: 0–360°, clockwise) converted to
 * counterclockwise radians for all internal calculations.  The absolute value
 * cancels when computing the error (target − current), so no reset-reference
 * offset is needed here.
 *
 * <h2>Default PID gains</h2>
 * <ul>
 *   <li>Kp = 1.5  rad/s per rad</li>
 *   <li>Ki = 0.2  rad/s per rad·s  (with ±π windup clamp)</li>
 *   <li>Kd = 0.3  rad/s per rad/s</li>
 * </ul>
 * Tune via {@link #setGains} or the constructor overload.
 */
public class HeadingStabilizer implements StabilizationModule {

    // ω below this threshold is treated as "go straight" (rad/s)
    private static final double ROTATION_DEADBAND = 0.05;

    // Maximum correction magnitude to prevent overcorrection (rad/s)
    private static final double MAX_CORRECTION = 2.0;

    // Integral windup clamp (rad)
    private static final double MAX_INTEGRAL = Math.PI;

    // Default PID gains
    private static final double DEFAULT_KP = 1.5;
    private static final double DEFAULT_KI = 0.2;
    private static final double DEFAULT_KD = 0.3;

    private final MotionControlModule inner;
    private final IMUModule           imu;

    // PID gains — volatile so setGains() is visible across threads
    private volatile double kp = DEFAULT_KP;
    private volatile double ki = DEFAULT_KI;
    private volatile double kd = DEFAULT_KD;

    // PID state — guarded by 'this'
    private double targetHeadingRad;
    private double integralError;
    private double lastError;
    private long   lastMoveNs;

    /**
     * @param inner wrapped motion module that executes the final motor commands
     * @param imu   IMU providing real-time heading
     */
    public HeadingStabilizer(MotionControlModule inner, IMUModule imu) {
        this(inner, imu, DEFAULT_KP, DEFAULT_KI, DEFAULT_KD);
    }

    /**
     * @param inner wrapped motion module
     * @param imu   IMU providing real-time heading
     * @param kp    proportional gain
     * @param ki    integral gain
     * @param kd    derivative gain
     */
    public HeadingStabilizer(MotionControlModule inner, IMUModule imu,
                              double kp, double ki, double kd) {
        this.inner = inner;
        this.imu   = imu;
        this.kp    = kp;
        this.ki    = ki;
        this.kd    = kd;

        targetHeadingRad = readHeadingRad();
        lastMoveNs       = System.nanoTime();
    }

    // -------------------------------------------------------------------------
    // StabilizationModule interface
    // -------------------------------------------------------------------------

    @Override
    public synchronized void setTargetHeading(double targetRad) {
        targetHeadingRad = targetRad;
        integralError    = 0;
        lastError        = 0;
    }

    @Override
    public void setGains(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    // -------------------------------------------------------------------------
    // MotionControlModule interface
    // -------------------------------------------------------------------------

    /**
     * Apply heading-hold correction then delegate to the wrapped module.
     *
     * <p>When {@code |omega| > deadband}: intentional rotation — pass through
     * unchanged and track the current heading as the new target.
     * <p>When {@code |omega| ≤ deadband}: compute PID correction and add to omega.
     */
    @Override
    public synchronized void move(double vx, double vy, double omega) {
        long   now = System.nanoTime();
        double dt  = Math.min((now - lastMoveNs) * 1e-9, 0.1); // clamp large gaps
        if (dt <= 0) dt = 0.01;
        lastMoveNs = now;

        double currentHeading = readHeadingRad();
        double correctedOmega;

        if (Math.abs(omega) > ROTATION_DEADBAND) {
            // Intentional rotation: track heading, no correction
            targetHeadingRad = currentHeading;
            integralError    = 0;
            lastError        = 0;
            correctedOmega   = omega;
        } else {
            // Heading-hold: PID on error = target − current
            double error = normalizeAngle(targetHeadingRad - currentHeading);

            integralError = clamp(integralError + error * dt, -MAX_INTEGRAL, MAX_INTEGRAL);
            double derivative = (error - lastError) / dt;
            lastError = error;

            double correction = kp * error + ki * integralError + kd * derivative;
            correction = clamp(correction, -MAX_CORRECTION, MAX_CORRECTION);

            correctedOmega = omega + correction;
        }

        inner.move(vx, vy, correctedOmega);
    }

    @Override
    public void stop() {
        inner.stop();
    }

    @Override
    public void freeRun() {
        inner.freeRun();
    }

    @Override
    public void close() {
        inner.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Read IMU heading and convert to counterclockwise radians. */
    private double readHeadingRad() {
        // BNO055 heading: 0–360° clockwise → negate for CCW convention
        return -Math.toRadians(imu.readData().orientation().heading());
    }

    /** Normalize angle to (−π, +π]. */
    private static double normalizeAngle(double angle) {
        while (angle >  Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
