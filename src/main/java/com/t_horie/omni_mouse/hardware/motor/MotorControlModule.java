package com.t_horie.omni_mouse.hardware.motor;

/**
 * Hardware abstraction layer for stepper motor driver.
 * Supports single or daisy-chained motor configurations.
 */
public interface MotorControlModule extends AutoCloseable {

    /** Returns the number of motors managed by this module. */
    int getMotorCount();

    /**
     * Run all motors at specified speeds.
     *
     * @param revsPerSec speed per motor (positive = forward, negative = reverse).
     *                   Array length must equal {@link #getMotorCount()}.
     */
    void run(double[] revsPerSec);

    /** Stop all motors with active braking (holds shaft position). */
    void stop();

    /** Release all motor coils (no holding torque). */
    void freeRun();

    @Override
    void close();
}
