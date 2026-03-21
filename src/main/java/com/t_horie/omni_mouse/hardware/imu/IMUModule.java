package com.t_horie.omni_mouse.hardware.imu;

/**
 * Hardware abstraction layer for IMU (Inertial Measurement Unit) sensor.
 * Provides access to orientation, acceleration, and angular velocity data.
 */
public interface IMUModule {
    /**
     * Read current IMU sensor data.
     *
     * @return IMU data containing orientation, acceleration, and angular velocity
     * @throws RuntimeException if reading fails
     */
    IMUData readData();

    /**
     * Shutdown the IMU module and release resources.
     */
    void shutdown();
}
