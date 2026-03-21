package com.t_horie.omni_mouse.sensing.odometry;

/**
 * Odometry data containing pose and velocity estimates.
 */
public record OdometryData(
        Pose pose,
        Velocity velocity,
        long timestampNanos
) {
    @Override
    public String toString() {
        return String.format("Odom[%s, %s]", pose, velocity);
    }
}
