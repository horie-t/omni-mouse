package com.t_horie.omni_mouse.sensing.odometry;

/**
 * Robot pose (position and orientation) in 2D space.
 */
public record Pose(
        double x,      // meters
        double y,      // meters
        double theta   // radians (heading angle)
) {
    /**
     * Create a new pose at origin.
     */
    public static Pose zero() {
        return new Pose(0.0, 0.0, 0.0);
    }

    /**
     * Normalize theta to [-π, π] range.
     */
    public Pose normalize() {
        double normalizedTheta = theta;
        while (normalizedTheta > Math.PI) normalizedTheta -= 2 * Math.PI;
        while (normalizedTheta < -Math.PI) normalizedTheta += 2 * Math.PI;
        return new Pose(x, y, normalizedTheta);
    }

    @Override
    public String toString() {
        return String.format("Pose(x=%.4f m, y=%.4f m, θ=%.2f°)", x, y, Math.toDegrees(theta));
    }
}
