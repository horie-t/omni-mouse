package com.t_horie.omni_mouse.sensor;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

public class Bno055Sensor {
    private static final int BNO055_ADDRESS = 0x28;
    private static final int BNO055_CHIP_ID_ADDR = 0x00;
    private static final int BNO055_OPR_MODE_ADDR = 0x3D;
    private static final int BNO055_PWR_MODE_ADDR = 0x3E;
    private static final int BNO055_SYS_TRIGGER_ADDR = 0x3F;

    // Operation modes
    private static final int OPERATION_MODE_CONFIG = 0x00;
    private static final int OPERATION_MODE_NDOF = 0x0C;

    // Power modes
    private static final int POWER_MODE_NORMAL = 0x00;

    // Data registers
    private static final int BNO055_EULER_H_LSB_ADDR = 0x1A;
    private static final int BNO055_QUATERNION_DATA_W_LSB_ADDR = 0x20;
    private static final int BNO055_ACCEL_DATA_X_LSB_ADDR = 0x08;
    private static final int BNO055_GYRO_DATA_X_LSB_ADDR = 0x14;

    private final I2C i2c;

    public Bno055Sensor(Context pi4j, int bus) {
        I2CConfig config = I2C.newConfigBuilder(pi4j)
                .id("BNO055")
                .bus(bus)
                .device(BNO055_ADDRESS)
                .build();

        this.i2c = pi4j.i2c().create(config);
        initialize();
    }

    private void initialize() {
        try {
            // Check chip ID
            int chipId = i2c.readRegister(BNO055_CHIP_ID_ADDR);
            if (chipId != 0xA0) {
                throw new RuntimeException("Invalid BNO055 chip ID: 0x" + Integer.toHexString(chipId));
            }

            // Set to config mode
            i2c.writeRegister(BNO055_OPR_MODE_ADDR, OPERATION_MODE_CONFIG);
            Thread.sleep(30);

            // Reset
            i2c.writeRegister(BNO055_SYS_TRIGGER_ADDR, 0x20);
            Thread.sleep(650);

            // Set power mode to normal
            i2c.writeRegister(BNO055_PWR_MODE_ADDR, POWER_MODE_NORMAL);
            Thread.sleep(10);

            // Set to NDOF mode
            i2c.writeRegister(BNO055_OPR_MODE_ADDR, OPERATION_MODE_NDOF);
            Thread.sleep(20);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BNO055", e);
        }
    }

    public SensorData readData() {
        try {
            // Read Euler angles (6 bytes: heading, roll, pitch)
            byte[] eulerData = new byte[6];
            i2c.readRegister(BNO055_EULER_H_LSB_ADDR, eulerData, 0, 6);

            double heading = ((eulerData[1] << 8) | (eulerData[0] & 0xFF)) / 16.0;
            double roll = ((eulerData[3] << 8) | (eulerData[2] & 0xFF)) / 16.0;
            double pitch = ((eulerData[5] << 8) | (eulerData[4] & 0xFF)) / 16.0;

            // Read quaternion (8 bytes: w, x, y, z)
            byte[] quatData = new byte[8];
            i2c.readRegister(BNO055_QUATERNION_DATA_W_LSB_ADDR, quatData, 0, 8);

            double w = ((short)((quatData[1] << 8) | (quatData[0] & 0xFF))) / 16384.0;
            double x = ((short)((quatData[3] << 8) | (quatData[2] & 0xFF))) / 16384.0;
            double y = ((short)((quatData[5] << 8) | (quatData[4] & 0xFF))) / 16384.0;
            double z = ((short)((quatData[7] << 8) | (quatData[6] & 0xFF))) / 16384.0;

            // Read accelerometer (6 bytes: x, y, z)
            byte[] accelData = new byte[6];
            i2c.readRegister(BNO055_ACCEL_DATA_X_LSB_ADDR, accelData, 0, 6);

            double accelX = ((short)((accelData[1] << 8) | (accelData[0] & 0xFF))) / 100.0;
            double accelY = ((short)((accelData[3] << 8) | (accelData[2] & 0xFF))) / 100.0;
            double accelZ = ((short)((accelData[5] << 8) | (accelData[4] & 0xFF))) / 100.0;

            // Read gyroscope (6 bytes: x, y, z)
            byte[] gyroData = new byte[6];
            i2c.readRegister(BNO055_GYRO_DATA_X_LSB_ADDR, gyroData, 0, 6);

            double gyroX = ((short)((gyroData[1] << 8) | (gyroData[0] & 0xFF))) / 16.0;
            double gyroY = ((short)((gyroData[3] << 8) | (gyroData[2] & 0xFF))) / 16.0;
            double gyroZ = ((short)((gyroData[5] << 8) | (gyroData[4] & 0xFF))) / 16.0;

            return new SensorData(
                    heading, roll, pitch,
                    w, x, y, z,
                    accelX, accelY, accelZ,
                    gyroX, gyroY, gyroZ
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to read sensor data", e);
        }
    }

    public void shutdown() {
        i2c.close();
    }

    public record SensorData(
            double heading, double roll, double pitch,
            double quatW, double quatX, double quatY, double quatZ,
            double accelX, double accelY, double accelZ,
            double gyroX, double gyroY, double gyroZ
    ) {
        @Override
        public String toString() {
            return String.format(
                    "Euler(h=%.2f, r=%.2f, p=%.2f) Quat(w=%.4f, x=%.4f, y=%.4f, z=%.4f) " +
                    "Accel(x=%.2f, y=%.2f, z=%.2f) Gyro(x=%.2f, y=%.2f, z=%.2f)",
                    heading, roll, pitch, quatW, quatX, quatY, quatZ,
                    accelX, accelY, accelZ, gyroX, gyroY, gyroZ
            );
        }
    }
}
