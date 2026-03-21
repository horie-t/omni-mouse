package com.t_horie.omni_mouse.hardware.imu;

/**
 * IMU sensor data containing orientation, acceleration, and angular velocity.
 */
public record IMUData(
        Orientation orientation,
        Acceleration acceleration,
        AngularVelocity angularVelocity
) {
    public record Orientation(
            double heading,  // degrees (yaw)
            double roll,     // degrees
            double pitch,    // degrees
            Quaternion quaternion
    ) {}

    public record Quaternion(
            double w,
            double x,
            double y,
            double z
    ) {}

    public record Acceleration(
            double x,  // m/s²
            double y,  // m/s²
            double z   // m/s²
    ) {}

    public record AngularVelocity(
            double x,  // deg/s
            double y,  // deg/s
            double z   // deg/s
    ) {}

    @Override
    public String toString() {
        return String.format(
                "Euler(h=%.2f, r=%.2f, p=%.2f) Quat(w=%.4f, x=%.4f, y=%.4f, z=%.4f) " +
                "Accel(x=%.2f, y=%.2f, z=%.2f) Gyro(x=%.2f, y=%.2f, z=%.2f)",
                orientation.heading, orientation.roll, orientation.pitch,
                orientation.quaternion.w, orientation.quaternion.x,
                orientation.quaternion.y, orientation.quaternion.z,
                acceleration.x, acceleration.y, acceleration.z,
                angularVelocity.x, angularVelocity.y, angularVelocity.z
        );
    }
}
