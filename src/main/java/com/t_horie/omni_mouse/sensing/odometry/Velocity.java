package com.t_horie.omni_mouse.sensing.odometry;

/**
 * Robot velocity in 2D space.
 */
public record Velocity(
        double vx,     // m/s (forward velocity)
        double vy,     // m/s (lateral velocity)
        double omega   // rad/s (angular velocity)
) {
    /**
     * Create a zero velocity.
     */
    public static Velocity zero() {
        return new Velocity(0.0, 0.0, 0.0);
    }

    @Override
    public String toString() {
        return String.format("Vel(vx=%.3f m/s, vy=%.3f m/s, ω=%.2f°/s)",
                vx, vy, Math.toDegrees(omega));
    }
}
