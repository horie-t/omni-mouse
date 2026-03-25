package com.t_horie.omni_mouse.hardware.camera;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

/**
 * rpicam-vid を使ったカメラモジュール実装。
 * MJPEG ストリームを stdout 経由で受け取り、JPEG フレーム境界を検出して
 * OpenCV imdecode でデコードする。
 *
 * <p>使用例:
 * <pre>{@code
 * RpicamCameraModule camera = new RpicamCameraModule(0, 640, 480, 30);
 * camera.start();
 * try {
 *     CameraFrame frame = camera.captureFrame();
 *     // ... 画像処理 ...
 *     frame.release();
 * } finally {
 *     camera.shutdown();
 * }
 * }</pre>
 */
public class RpicamCameraModule implements DownwardCameraModule {

    private static final int JPEG_SOI_FF = 0xFF;
    private static final int JPEG_SOI_D8 = 0xD8;
    private static final int JPEG_EOI_D9 = 0xD9;

    private final int cameraIndex;
    private final int width;
    private final int height;
    private final int framerate;

    private Process rpicamProcess;
    private BufferedInputStream cameraStream;

    /**
     * @param cameraIndex カメラインデックス（1台なら 0）
     * @param width       撮影幅 (px)
     * @param height      撮影高さ (px)
     * @param framerate   フレームレート (fps)
     */
    public RpicamCameraModule(int cameraIndex, int width, int height, int framerate) {
        this.cameraIndex = cameraIndex;
        this.width = width;
        this.height = height;
        this.framerate = framerate;
    }

    @Override
    public void start() {
        try {
            rpicamProcess = new ProcessBuilder(
                    "rpicam-vid",
                    "--camera", String.valueOf(cameraIndex),
                    "--codec", "mjpeg",
                    "--width", String.valueOf(width),
                    "--height", String.valueOf(height),
                    "--framerate", String.valueOf(framerate),
                    "--output", "-",
                    "--inline",
                    "--nopreview",
                    "-t", "0"
            ).redirectError(ProcessBuilder.Redirect.DISCARD).start();

            cameraStream = new BufferedInputStream(rpicamProcess.getInputStream(), 1024 * 1024);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start rpicam-vid", e);
        }
    }

    @Override
    public CameraFrame captureFrame() {
        try {
            byte[] jpegBytes = readNextJpegFrame();

            try (Mat buf = new Mat(1, jpegBytes.length, CV_8UC1);
                 BytePointer ptr = new BytePointer(jpegBytes)) {
                buf.data(ptr);
                Mat image = imdecode(buf, IMREAD_COLOR);
                if (image.empty()) {
                    throw new RuntimeException("Failed to decode JPEG frame");
                }
                return new CameraFrame(image, Instant.now());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read camera stream", e);
        }
    }

    /**
     * ストリームから次の JPEG フレームを読み取る。
     * JPEG は FF D8 で始まり FF D9 で終わる。
     */
    private byte[] readNextJpegFrame() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(width * height);
        boolean inFrame = false;
        int prev = -1;

        while (true) {
            int b = cameraStream.read();
            if (b == -1) {
                throw new IOException("Camera stream ended unexpectedly");
            }

            if (!inFrame) {
                if (prev == JPEG_SOI_FF && b == JPEG_SOI_D8) {
                    inFrame = true;
                    buf.write(JPEG_SOI_FF);
                    buf.write(JPEG_SOI_D8);
                }
            } else {
                buf.write(b);
                if (prev == JPEG_SOI_FF && b == JPEG_EOI_D9) {
                    return buf.toByteArray();
                }
            }
            prev = b;
        }
    }

    @Override
    public void shutdown() {
        if (rpicamProcess != null) {
            rpicamProcess.destroy();
        }
    }
}
