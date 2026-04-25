package com.t_horie.omni_mouse.hardware.imu;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;

/**
 * Bno055IMUModule の動作確認用スタンドアロンプログラム。
 * 指定回数だけ IMU データを読み取りコンソールに表示する。
 *
 * 実行方法 (Raspberry Pi 上):
 *   java -Dloader.main=com.t_horie.omni_mouse.hardware.imu.IMUReadTest \
 *     -jar omni-mouse-0.2.4-SNAPSHOT.jar [count] [intervalMs]
 *
 * オプション引数:
 *   args[0] - 読み取り回数       (デフォルト: 10)
 *   args[1] - 読み取り間隔 [ms]  (デフォルト: 500)
 */
public class IMUReadTest {

    private static final int I2C_BUS = 1;

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        long intervalMs = args.length > 1 ? Long.parseLong(args[1]) : 500;

        System.out.println("I2C bus       : " + I2C_BUS);
        System.out.println("Read count    : " + count);
        System.out.println("Interval [ms] : " + intervalMs);

        Context pi4j = Pi4J.newAutoContext();
        Bno055IMUModule imu = new Bno055IMUModule(pi4j, I2C_BUS);
        System.out.println("IMU initialized. Reading data...");

        try {
            for (int i = 1; i <= count; i++) {
                IMUData data = imu.readData();
                System.out.printf("[%3d/%d] %s%n", i, count, data);
                if (i < count) {
                    Thread.sleep(intervalMs);
                }
            }
            System.out.println("Done.");
        } finally {
            imu.shutdown();
            pi4j.shutdown();
        }
    }
}
