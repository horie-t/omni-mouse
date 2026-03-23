package com.t_horie.omni_mouse.control.motion;

import com.t_horie.omni_mouse.hardware.motor.MotorControlModule;

/**
 * Omnidirectional motion controller for a 3-wheel omni robot.
 *
 * <h2>Wheel layout (default)</h2>
 * <pre>
 *          [W0] φ=0°
 *            ↑ (front)
 *           / \
 *          /   \
 *    [W2] /     \ [W1]
 *   φ=240°       φ=120°
 * </pre>
 * All wheels are 120° apart, at radius {@value #CENTER_RADIUS_M} m from the robot centre.
 *
 * <h2>Inverse kinematics</h2>
 * For wheel i at position angle φᵢ (measured counterclockwise from robot +x/forward):
 * <pre>
 *   v_wheel_i = −sin(φᵢ)·Vx + cos(φᵢ)·Vy + r·ω
 *   revsPerSec[i] = v_wheel_i / (2π × r_wheel)
 * </pre>
 * The positive wheel direction corresponds to counterclockwise motion around the robot.
 */
public class OmniMotionModule implements MotionControlModule {

    /** Distance from robot centre to each wheel contact point (m). */
    public static final double CENTER_RADIUS_M = 0.050;

    /** Wheel radius = diameter 48 mm / 2 (m). */
    public static final double WHEEL_RADIUS_M = 0.024;

    /**
     * Position angles of the three wheels in degrees, counterclockwise from robot forward (+x).
     * motor[0] → 0° (front), motor[1] → 120° (back-left), motor[2] → 240° (back-right).
     */
    private static final double[] WHEEL_ANGLES_DEG = {0.0, 120.0, 240.0};

    private final MotorControlModule motors;

    // Pre-computed sin/cos of wheel angles to avoid repeated trig calls
    private final double[] sinPhi = new double[3];
    private final double[] cosPhi = new double[3];

    public OmniMotionModule(MotorControlModule motors) {
        this.motors = motors;
        for (int i = 0; i < 3; i++) {
            double rad = Math.toRadians(WHEEL_ANGLES_DEG[i]);
            sinPhi[i] = Math.sin(rad);
            cosPhi[i] = Math.cos(rad);
        }
    }

    /**
     * Command robot body velocity in the robot frame.
     *
     * @param vx    forward velocity (m/s)
     * @param vy    leftward velocity (m/s)
     * @param omega angular velocity (rad/s), positive = counterclockwise
     */
    @Override
    public void move(double vx, double vy, double omega) {
        double[] revsPerSec = inverseKinematics(vx, vy, omega);
        motors.run(revsPerSec);
    }

    @Override
    public void stop() {
        motors.stop();
    }

    @Override
    public void freeRun() {
        motors.freeRun();
    }

    @Override
    public void close() {
        motors.close();
    }

    // -------------------------------------------------------------------------
    // Inverse kinematics
    // -------------------------------------------------------------------------

    /**
     * Compute per-wheel speeds from robot body velocity.
     *
     * <p>Derivation: the velocity component of the robot at wheel i's contact point,
     * projected onto the wheel's rolling direction (tangential, counterclockwise):
     * <pre>
     *   v_i = −sin(φᵢ)·Vx + cos(φᵢ)·Vy + r_centre·ω
     * </pre>
     *
     * @return revsPerSec[0..2] for motor[0..2]; positive = counterclockwise tangential roll
     */
    double[] inverseKinematics(double vx, double vy, double omega) {
        double[] revsPerSec = new double[3];
        for (int i = 0; i < 3; i++) {
            double wheelLinearVelocity = -sinPhi[i] * vx + cosPhi[i] * vy + CENTER_RADIUS_M * omega;
            revsPerSec[i] = wheelLinearVelocity / (2.0 * Math.PI * WHEEL_RADIUS_M);
        }
        return revsPerSec;
    }
}
