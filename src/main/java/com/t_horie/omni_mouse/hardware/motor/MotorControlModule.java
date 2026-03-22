package com.t_horie.omni_mouse.hardware.motor;

/**
 * Hardware abstraction layer for stepper motor driver.
 * Controls a single stepper motor via SPI.
 */
public interface MotorControlModule extends AutoCloseable {

    /**
     * Run the motor at the specified speed.
     *
     * @param revsPerSec revolutions per second (positive = forward, negative = reverse)
     */
    void run(double revsPerSec);

    /**
     * Stop the motor (active braking, holds position).
     */
    void stop();

    /**
     * Put motor outputs in high-impedance state (no holding torque).
     */
    void freeRun();

    @Override
    void close();
}
