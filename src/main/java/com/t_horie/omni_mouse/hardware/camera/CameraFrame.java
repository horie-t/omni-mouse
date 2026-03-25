package com.t_horie.omni_mouse.hardware.camera;

import org.bytedeco.opencv.opencv_core.Mat;

import java.time.Instant;

/**
 * 1フレーム分のカメラ画像データ。
 * image() が返す Mat はネイティブメモリを保持するため、
 * 使用後に release() を呼び出してリソースを解放すること。
 */
public final class CameraFrame {

    private final Mat image;
    private final Instant timestamp;

    public CameraFrame(Mat image, Instant timestamp) {
        this.image = image;
        this.timestamp = timestamp;
    }

    /** OpenCV Mat 形式の画像データ。 */
    public Mat image() {
        return image;
    }

    /** フレーム取得時刻。 */
    public Instant timestamp() {
        return timestamp;
    }

    /** ネイティブメモリを解放する。使用後に必ず呼び出すこと。 */
    public void release() {
        if (image != null && !image.isNull()) {
            image.release();
        }
    }
}
