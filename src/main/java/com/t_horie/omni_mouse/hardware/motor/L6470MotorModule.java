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
 * <h2>Daisy-chain SPI protocol</h2>
 * An M-byte command to N motors requires M separate CS transactions (one per byte
 * position). Each transaction contains exactly N bytes:
 * {@code [motor[N-1].byte[k], ..., motor[1].byte[k], motor[0].byte[k]]}
 * Because motor[0] (closest to MOSI) receives the LAST byte of each N-byte group
 * (standard shift-register behaviour), and motor[N-1] (closest to MISO) receives
 * the first.
 *
 * <h2>SPI settings</h2>
 * Mode 3 (CPOL=1, CPHA=1), MSB first, up to 5 MHz (L6470 datasheet Table 15).
 *
 * <h2>Speed register</h2>
 * speed_reg = steps_per_sec × 250 ns × 2^28  (datasheet section 9.1.4)
 */
public class L6470MotorModule implements MotorControlModule, MotorEncoderModule {

    public static final int MOTOR_COUNT = 3;

    // --- Register addresses ---
    private static final byte REG_ABS_POS  = 0x01;  // 22-bit signed position in steps
    private static final byte REG_STEP_MODE = 0x16;
    private static final byte REG_KVAL_HOLD = 0x09;
    private static final byte REG_KVAL_RUN  = 0x0A;
    private static final byte REG_KVAL_ACC  = 0x0B;
    private static final byte REG_KVAL_DEC  = 0x0C;

    // --- Commands ---
    private static final byte CMD_RUN_FORWARD     = 0x51;       // Run forward
    private static final byte CMD_RUN_REVERSE     = 0x50;       // Run reverse
    private static final byte CMD_HARD_STOP       = (byte) 0xB8; // Stop with braking
    private static final byte CMD_HARD_HIZ        = (byte) 0xA8; // Coils off
    private static final byte CMD_RESET_DEVICE    = (byte) 0xC0; // Soft reset
    private static final byte CMD_GET_PARAM_ABS_POS = (byte) (0x20 | 0x01); // GetParam ABS_POS

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
    // MotorEncoderModule interface
    // -------------------------------------------------------------------------

    /**
     * Read the absolute position of all motors from the L6470 ABS_POS register.
     *
     * <p>ABS_POS is a 22-bit signed integer (steps from the power-on reference).
     * After device reset, ABS_POS = 0. In full-step mode, each step increments
     * or decrements the count by 1.
     *
     * <p>Daisy-chain read sequence (3 motors, 3 response bytes per motor):
     * <ol>
     *   <li>1 CS transaction: send GetParam command [0x21 × 3]</li>
     *   <li>3 CS transactions: send dummy bytes, receive 1 byte per motor each time</li>
     * </ol>
     *
     * @return absolute positions in revolutions; positive = forward direction
     */
    @Override
    public double[] getAbsolutePositions() {
        // Issue GetParam ABS_POS to all motors
        sendUniform(CMD_GET_PARAM_ABS_POS, 1);

        // Read 3-byte response from each motor (22-bit value, MSB first)
        byte[][] raw = receiveMultiByte(3);

        double[] revolutions = new double[MOTOR_COUNT];
        for (int i = 0; i < MOTOR_COUNT; i++) {
            int pos24 = ((raw[i][0] & 0xFF) << 16)
                      | ((raw[i][1] & 0xFF) << 8)
                      |  (raw[i][2] & 0xFF);
            // Sign-extend from bit 21 to 32-bit int
            int steps = (pos24 & 0x3F_FFFF) << 10 >> 10;
            revolutions[i] = (double) steps / STEPS_PER_REV;
        }
        return revolutions;
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
     * <p>Each byte position of the M-byte command is sent as a dedicated CS
     * transaction containing N bytes (one per motor). This is required by the
     * L6470: a rising CS edge between byte groups is how the daisy chain
     * routes each byte to the correct chip.
     *
     * @param cmds cmds[motorIndex][byteIndex] — all rows must have equal length
     */
    private void sendMultiByte(byte[][] cmds) {
        int bytesPerCmd = cmds[0].length;

        for (int k = 0; k < bytesPerCmd; k++) {
            byte[] group = new byte[MOTOR_COUNT];
            for (int i = 0; i < MOTOR_COUNT; i++) {
                group[MOTOR_COUNT - 1 - i] = cmds[i][k];
            }
            spi.write(group);  // one CS pulse per byte position
        }
    }

    /**
     * Receive response bytes from all motors after a read command.
     *
     * <p>Sends dummy bytes (0x00) in each CS transaction and captures MISO data.
     * Byte ordering on MISO mirrors the send direction:
     * {@code rx[MOTOR_COUNT-1-i]} is motor[i]'s byte, because motor[0]
     * (closest to MOSI) must propagate through all chips before reaching MISO.
     *
     * @param bytesPerResponse bytes per motor in the response
     * @return raw[motorIndex][byteIndex]
     */
    private byte[][] receiveMultiByte(int bytesPerResponse) {
        byte[] txDummy = new byte[MOTOR_COUNT]; // zeros — ignored by chips in response phase
        byte[] rxBuf   = new byte[MOTOR_COUNT];
        byte[][] result = new byte[MOTOR_COUNT][bytesPerResponse];

        for (int k = 0; k < bytesPerResponse; k++) {
            spi.transfer(txDummy, rxBuf); // one CS pulse, full-duplex
            for (int i = 0; i < MOTOR_COUNT; i++) {
                result[i][k] = rxBuf[MOTOR_COUNT - 1 - i]; // motor[0] is last on MISO
            }
        }
        return result;
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
