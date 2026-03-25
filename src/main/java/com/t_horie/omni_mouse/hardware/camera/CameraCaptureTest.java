package com.t_horie.omni_mouse.hardware.camera;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camera Module 3 の動作確認用スタンドアロンプログラム。
 * rpicam-still を使って画像を1枚撮影して JPEG ファイルに保存する。
 *
 * 実行方法 (Raspberry Pi 上):
 *   java -Dloader.main=com.t_horie.omni_mouse.hardware.camera.CameraCaptureTest \
 *     -jar omni-mouse-0.2.0.jar [outputPath]
 *
 * オプション引数:
 *   args[0] - 出力ファイルパス (デフォルト: capture_yyyyMMdd_HHmmss.jpg)
 */
public class CameraCaptureTest {

    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : generateFilename();

        System.out.println("Output file   : " + outputPath);

        ProcessBuilder pb = new ProcessBuilder(
                "rpicam-still",
                "--output", outputPath,
                "--immediate",
                "--nopreview"
        );
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("ERROR: rpicam-still exited with code " + exitCode);
            System.exit(1);
        }

        File outputFile = new File(outputPath);
        if (outputFile.exists()) {
            System.out.printf("Saved: %s (%,d bytes)%n", outputPath, outputFile.length());
        } else {
            System.err.println("ERROR: Output file not found.");
            System.exit(1);
        }
    }

    private static String generateFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "capture_" + timestamp + ".jpg";
    }
}
