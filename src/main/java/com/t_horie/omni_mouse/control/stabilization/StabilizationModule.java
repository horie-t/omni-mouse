package com.t_horie.omni_mouse.control.stabilization;

import com.t_horie.omni_mouse.control.motion.MotionControlModule;

/**
 * Heading-stabilization layer that sits between path following and motion control.
 *
 * <p>Decorates a {@link MotionControlModule}: receives the same
 * {@code move(vx, vy, ω)} call, injects a heading-hold correction (Δω), and
 * forwards the adjusted command to the wrapped module.
 *
 * <p>Data-flow role: {@code STAB → MOTION : 補正値 (Δω)}
 */
public interface StabilizationModule extends MotionControlModule {

    /**
     * Override the current target heading.
     * Calling this also resets the integral accumulator.
     *
     * @param targetRad desired heading in radians (CCW positive, robot-frame convention)
     */
    void setTargetHeading(double targetRad);

    /**
     * Tune PID gains at runtime (e.g. from configuration service).
     *
     * @param kp proportional gain (rad/s per rad error)
     * @param ki integral gain    (rad/s per rad·s)
     * @param kd derivative gain  (rad/s per rad/s)
     */
    void setGains(double kp, double ki, double kd);
}
