package com.t_horie.omni_mouse.hardware.camera;

import org.bytedeco.opencv.opencv_core.Mat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * DownwardCameraModule (RpicamCameraModule) の動作確認用スタンドアロンプログラム。
 * 1フレームを取得して JPEG ファイルに保存する。
 *
 * 実行方法 (Raspberry Pi 上):
 *   java -Dloader.main=com.t_horie.omni_mouse.hardware.camera.CameraCaptureTest \
 *     -jar omni-mouse-0.2.2-SNAPSHOT.jar [cameraIndex] [outputPath]
 *
 * オプション引数:
 *   args[0] - カメラインデックス (デフォルト: 0)
 *   args[1] - 出力ファイルパス   (デフォルト: capture_yyyyMMdd_HHmmss.jpg)
 */
public class CameraCaptureTest {

    public static void main(String[] args) throws Exception {
        int cameraIndex = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        String outputPath = args.length > 1 ? args[1] : generateFilename();

        System.out.println("Camera index  : " + cameraIndex);
        System.out.println("Output file   : " + outputPath);

        RpicamCameraModule camera = new RpicamCameraModule(cameraIndex, 640, 480, 30);
        camera.start();
        System.out.println("Camera started. Capturing frame...");

        CameraFrame frame = camera.captureFrame();
        try {
            Mat mat = frame.image();
            System.out.printf("Frame captured: %dx%d%n", mat.cols(), mat.rows());

            boolean saved = imwrite(outputPath, mat);
            if (saved) {
                System.out.println("Saved: " + outputPath);
            } else {
                System.err.println("ERROR: Failed to save image.");
                System.exit(1);
            }
        } finally {
            frame.release();
            camera.shutdown();
        }
    }

    private static String generateFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "capture_" + timestamp + ".jpg";
    }
}
