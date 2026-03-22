package com.t_horie.omni_mouse.hardware.motor;

import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.io.spi.SpiBus;

/**
 * L6470 stepper motor driver — daisy-chain implementation.
 *
 * <h2>Daisy-chain wiring</h2>
 * <pre>
 *   RPi MOSI → motor[0].SDI → motor[0].SDO
 *                            → motor[1].SDI → motor[1].SDO
 *                                           → motor[2].SDI → motor[2].SDO → RPi MISO
 *   RPi SCLK and CE shared by all three chips.
 * </pre>
 *
 * <h2>Byte ordering in daisy chain</h2>
 * For N motors and an M-byte command, N×M bytes are sent in one CS transaction.
 * Each byte position k is a group of N bytes ordered as:
 * {@code [motor[N-1].byte[k], ..., motor[1].byte[k], motor[0].byte[k]]}
 * i.e. motor[0] (closest to MOSI) receives the LAST byte of each group.
 *
 * <h2>SPI settings</h2>
 * Mode 3 (CPOL=1, CPHA=1), MSB first, up to 5 MHz (L6470 datasheet Table 15).
 *
 * <h2>Speed register</h2>
 * speed_reg = steps_per_sec × 250 ns × 2^28  (datasheet section 9.1.4)
 */
public class L6470MotorModule implements MotorControlModule {

    public static final int MOTOR_COUNT = 3;

    // --- Register addresses ---
    private static final byte REG_STEP_MODE = 0x16;
    private static final byte REG_KVAL_HOLD = 0x09;
    private static final byte REG_KVAL_RUN  = 0x0A;
    private static final byte REG_KVAL_ACC  = 0x0B;
    private static final byte REG_KVAL_DEC  = 0x0C;

    // --- Commands ---
    private static final byte CMD_NOP          = 0x00;
    private static final byte CMD_RUN_FORWARD  = 0x51;       // Run forward
    private static final byte CMD_RUN_REVERSE  = 0x50;       // Run reverse
    private static final byte CMD_HARD_STOP    = (byte) 0xB8; // Stop with braking
    private static final byte CMD_HARD_HIZ     = (byte) 0xA8; // Coils off
    private static final byte CMD_RESET_DEVICE = (byte) 0xC0; // Soft reset

    // --- Step mode ---
    private static final byte STEP_MODE_FULL = 0x00;

    // --- Physical constants ---
    private static final int    STEPS_PER_REV  = 200;    // typical 2-phase stepper
    private static final double TICK_DURATION_S = 250e-9; // 250 ns at 16 MHz

    private final Spi spi;

    /**
     * @param pi4j       Pi4J context
     * @param bus        SPI bus (BUS_0 = /dev/spidev0.x)
     * @param chipSelect chip-select line shared by all 3 motors (CE0 or CE1)
     * @param kval       coil voltage ratio for all motors (0x00–0xFF = 0–100% of Vcc).
     *                   Start with 0x40 (25%) and adjust for your motor/supply.
     */
    public L6470MotorModule(Context pi4j, SpiBus bus, SpiChipSelect chipSelect, byte kval) {
        var spiConfig = Spi.newConfigBuilder(pi4j)
                .id("L6470-" + bus + "-" + chipSelect)
                .bus(bus)
                .chipSelect(chipSelect)
                .baud(4_000_000)       // 4 MHz (max 5 MHz)
                .mode(SpiMode.MODE_3)  // CPOL=1, CPHA=1
                .build();
        this.spi = pi4j.spi().create(spiConfig);
        initialize(kval);
    }

    // -------------------------------------------------------------------------
    // MotorControlModule interface
    // -------------------------------------------------------------------------

    @Override
    public int getMotorCount() {
        return MOTOR_COUNT;
    }

    /**
     * Run all motors simultaneously.
     *
     * @param revsPerSec array of length 3; positive = forward, negative = reverse
     */
    @Override
    public void run(double[] revsPerSec) {
        if (revsPerSec.length != MOTOR_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + MOTOR_COUNT + " speeds, got " + revsPerSec.length);
        }

        // Build 4-byte command per motor: [cmd, speed_hi, speed_mid, speed_lo]
        byte[][] cmds = new byte[MOTOR_COUNT][4];
        for (int i = 0; i < MOTOR_COUNT; i++) {
            double spd = revsPerSec[i];
            int speedReg = toSpeedReg(Math.abs(spd) * STEPS_PER_REV);
            cmds[i][0] = spd >= 0 ? CMD_RUN_FORWARD : CMD_RUN_REVERSE;
            cmds[i][1] = (byte) ((speedReg >> 16) & 0x0F);
            cmds[i][2] = (byte) ((speedReg >> 8)  & 0xFF);
            cmds[i][3] = (byte) (speedReg         & 0xFF);
        }
        sendMultiByte(cmds);
    }

    @Override
    public void stop() {
        sendUniform(CMD_HARD_STOP, 1);
    }

    @Override
    public void freeRun() {
        sendUniform(CMD_HARD_HIZ, 1);
    }

    @Override
    public void close() {
        stop();
        freeRun();
        spi.close();
    }

    // -------------------------------------------------------------------------
    // Daisy-chain SPI helpers
    // -------------------------------------------------------------------------

    /**
     * Send the same single-byte command to all motors.
     *
     * @param cmd         command byte
     * @param bytesPerCmd total bytes this command occupies (1 for single-byte cmds)
     */
    private void sendUniform(byte cmd, int bytesPerCmd) {
        byte[][] cmds = new byte[MOTOR_COUNT][bytesPerCmd];
        for (int i = 0; i < MOTOR_COUNT; i++) {
            cmds[i][0] = cmd;
            // remaining bytes are already 0x00 (NOP / padding)
        }
        sendMultiByte(cmds);
    }

    /**
     * Send independent multi-byte commands to all motors simultaneously.
     *
     * <p>Daisy-chain byte layout for N motors and M bytes per command:<br>
     * {@code buf[k*N + (N-1-i)] = cmds[i][k]}
     * where {@code i} = motor index, {@code k} = byte position in command.
     *
     * @param cmds cmds[motorIndex][byteIndex] — all rows must have equal length
     */
    private void sendMultiByte(byte[][] cmds) {
        int bytesPerCmd = cmds[0].length;
        byte[] buf = new byte[MOTOR_COUNT * bytesPerCmd];

        for (int k = 0; k < bytesPerCmd; k++) {
            for (int i = 0; i < MOTOR_COUNT; i++) {
                // motor[0] (closest to MOSI) → last slot in each group
                // motor[N-1] (closest to MISO) → first slot in each group
                buf[k * MOTOR_COUNT + (MOTOR_COUNT - 1 - i)] = cmds[i][k];
            }
        }
        spi.write(buf);
    }

    /**
     * SetParam for a 1-byte register — same value for all motors.
     * Total bytes sent: 2 × N (command group + data group).
     */
    private void setParam1All(byte register, byte value) {
        byte[][] cmds = new byte[MOTOR_COUNT][2];
        for (int i = 0; i < MOTOR_COUNT; i++) {
            cmds[i][0] = register; // SetParam command = register address (bit 5 clear)
            cmds[i][1] = value;
        }
        sendMultiByte(cmds);
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    private void initialize(byte kval) {
        try {
            // Reset all chips simultaneously
            sendUniform(CMD_RESET_DEVICE, 1);
            Thread.sleep(1);

            setParam1All(REG_STEP_MODE, STEP_MODE_FULL);
            setParam1All(REG_KVAL_HOLD, kval);
            setParam1All(REG_KVAL_RUN,  kval);
            setParam1All(REG_KVAL_ACC,  kval);
            setParam1All(REG_KVAL_DEC,  kval);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("L6470 initialization interrupted", e);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /** steps/sec → L6470 SPEED register (20-bit). */
    private static int toSpeedReg(double stepsPerSec) {
        return (int) Math.round(stepsPerSec * TICK_DURATION_S * (1L << 28));
    }
}
