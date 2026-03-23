package com.t_horie.omni_mouse.hardware.motor;

/**
 * Hardware abstraction for reading motor position from the driver chip.
 *
 * <p>The absolute position is measured in revolutions from the reference point
 * established at power-on (or after a device reset).
 */
public interface MotorEncoderModule {

    /**
     * Read the current absolute position of all motors.
     *
     * @return positions in revolutions for each motor (positive = forward direction).
     *         Array length equals the number of motors.
     */
    double[] getAbsolutePositions();
}
