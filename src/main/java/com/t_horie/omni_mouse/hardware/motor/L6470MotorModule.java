package com.t_horie.omni_mouse.hardware.motor;

import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.io.spi.SpiBus;

/**
 * L6470 stepper motor driver implementation.
 *
 * <p>SPI settings: Mode 3 (CPOL=1, CPHA=1), MSB first, up to 5 MHz.
 *
 * <p>Speed register formula: speed_reg = steps_per_sec × 250ns × 2^28
 * (from L6470 datasheet section 9.1.4)
 */
public class L6470MotorModule implements MotorControlModule {

    // --- Register addresses ---
    private static final byte REG_STEP_MODE = 0x16;
    private static final byte REG_KVAL_HOLD = 0x09;
    private static final byte REG_KVAL_RUN  = 0x0A;
    private static final byte REG_KVAL_ACC  = 0x0B;
    private static final byte REG_KVAL_DEC  = 0x0C;

    // --- Commands ---
    private static final byte CMD_RUN_FORWARD  = 0x51;  // Run forward
    private static final byte CMD_RUN_REVERSE  = 0x50;  // Run reverse
    private static final byte CMD_HARD_STOP    = (byte) 0xB8;  // Stop (holds)
    private static final byte CMD_HARD_HIZ     = (byte) 0xA8;  // High-Z (free)
    private static final byte CMD_RESET_DEVICE = (byte) 0xC0;  // Soft reset

    // --- Step mode values ---
    private static final byte STEP_MODE_FULL   = 0x00;

    // --- Physical constants ---
    // Typical 2-phase stepper: 200 full steps per revolution
    private static final int STEPS_PER_REV = 200;
    // L6470 internal timer tick = 250 ns (at 16 MHz oscillator)
    private static final double TICK_DURATION_S = 250e-9;

    private final Spi spi;

    /**
     * @param pi4j        Pi4J context
     * @param bus         SPI bus (BUS_0 = /dev/spidev0.x)
     * @param chipSelect  Chip select line (CS_0 = CE0, CS_1 = CE1)
     * @param kval        Motor coil voltage ratio (0x00–0xFF = 0–100% of Vcc).
     *                    Adjust for your motor and supply voltage.
     *                    Example: 0x40 ≈ 25% of Vcc
     */
    public L6470MotorModule(Context pi4j, SpiBus bus, SpiChipSelect chipSelect, byte kval) {
        var spiConfig = Spi.newConfigBuilder(pi4j)
                .id("L6470-" + bus + "-" + chipSelect)
                .bus(bus)
                .chipSelect(chipSelect)
                .baud(4_000_000)       // 4 MHz (L6470 max: 5 MHz)
                .mode(SpiMode.MODE_3)  // CPOL=1, CPHA=1 per L6470 datasheet
                .build();
        this.spi = pi4j.spi().create(spiConfig);
        initialize(kval);
    }

    private void initialize(byte kval) {
        try {
            // Reset all registers to default values
            spi.write(CMD_RESET_DEVICE);
            Thread.sleep(1);  // Wait for reset to complete

            // Full step mode (SYNC_SEL=0, STEP_SEL=000)
            setParam1(REG_STEP_MODE, STEP_MODE_FULL);

            // Motor coil voltage (same value for hold/run/acc/dec for simplicity)
            setParam1(REG_KVAL_HOLD, kval);
            setParam1(REG_KVAL_RUN,  kval);
            setParam1(REG_KVAL_ACC,  kval);
            setParam1(REG_KVAL_DEC,  kval);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("L6470 initialization interrupted", e);
        }
    }

    /** SetParam command for 1-byte registers. */
    private void setParam1(byte register, byte value) {
        // CS is held low for the entire write() call
        spi.write(register, value);
    }

    /**
     * Run the motor continuously at the given speed.
     *
     * @param revsPerSec revolutions per second
     *                   positive = forward (DIR=1), negative = reverse (DIR=0)
     */
    @Override
    public void run(double revsPerSec) {
        double stepsPerSec = Math.abs(revsPerSec) * STEPS_PER_REV;
        int speedReg = toSpeedReg(stepsPerSec);

        byte cmd = revsPerSec >= 0 ? CMD_RUN_FORWARD : CMD_RUN_REVERSE;

        // Run command: 1 byte cmd + 3 bytes speed (20-bit, upper 4 bits unused)
        spi.write(
                cmd,
                (byte) ((speedReg >> 16) & 0x0F),  // bits 19-16 (4 bits)
                (byte) ((speedReg >> 8)  & 0xFF),  // bits 15-8
                (byte) (speedReg         & 0xFF)   // bits 7-0
        );
    }

    /**
     * Stop the motor with active braking (holds shaft position).
     */
    @Override
    public void stop() {
        spi.write(CMD_HARD_STOP);
    }

    /**
     * Release motor coils (no holding torque, shaft free to spin).
     */
    @Override
    public void freeRun() {
        spi.write(CMD_HARD_HIZ);
    }

    @Override
    public void close() {
        stop();
        freeRun();
        spi.close();
    }

    /**
     * Convert steps/sec to the L6470 SPEED register value.
     *
     * Formula (from datasheet): speed_reg = steps_per_sec × T_tick × 2^28
     * where T_tick = 250 ns
     */
    private static int toSpeedReg(double stepsPerSec) {
        return (int) Math.round(stepsPerSec * TICK_DURATION_S * (1L << 28));
    }
}
