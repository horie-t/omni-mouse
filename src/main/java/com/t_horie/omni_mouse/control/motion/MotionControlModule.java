package com.t_horie.omni_mouse.control.motion;

/**
 * Control layer interface for omnidirectional motion.
 *
 * <p>Coordinate convention (robot frame, right-hand system viewed from above):
 * <ul>
 *   <li>+x = forward
 *   <li>+y = left
 *   <li>+ω = counterclockwise
 * </ul>
 */
public interface MotionControlModule extends AutoCloseable {

    /**
     * Command robot body velocity.
     *
     * @param vx    forward velocity (m/s), positive = forward
     * @param vy    lateral velocity (m/s), positive = left
     * @param omega angular velocity (rad/s), positive = counterclockwise
     */
    void move(double vx, double vy, double omega);

    /** Stop all motors with active braking. */
    void stop();

    /** Release all motor coils (free-spin). */
    void freeRun();

    @Override
    void close();
}
