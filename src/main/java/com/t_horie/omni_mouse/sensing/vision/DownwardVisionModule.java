package com.t_horie.omni_mouse.sensing.vision;

import com.t_horie.omni_mouse.hardware.camera.CameraFrame;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * 見下ろしカメラ（FOV 102°）を使った壁検出モジュール。
 *
 * <p>アルゴリズム概要:
 * <ol>
 *   <li>グレースケール変換</li>
 *   <li>ガウシアンブラーでノイズ除去</li>
 *   <li>Canny エッジ検出</li>
 *   <li>前後左右の端部ストリップでエッジ密度を測定</li>
 *   <li>閾値を超えたストリップを壁ありと判定</li>
 * </ol>
 *
 * <p>座標系: カメラ画像の上方向 = ロボット前方。
 * カメラはロボット中心に下向きに固定されており、画像上端がロボット前方に対応することを前提とする。
 *
 * <p>スレッドセーフではない。単一スレッドから呼び出すこと（30Hz 制御ループ想定）。
 */
public class DownwardVisionModule implements VisionModule {

    /**
     * Canny エッジ検出の低閾値。
     * 値を下げると感度が上がりノイズを拾いやすくなる。実機で要調整。
     */
    private final double cannyThresholdLow;

    /**
     * Canny エッジ検出の高閾値。
     * 低閾値の 2〜3 倍が目安。
     */
    private final double cannyThresholdHigh;

    /**
     * 壁ありと判定するエッジ密度の閾値（0.0〜1.0）。
     * ストリップ内の (エッジピクセル数 / ストリップ面積) がこの値以上で壁ありと判定する。
     * 環境光や床・壁のコントラストに応じて実機で要調整。
     */
    private final double wallEdgeDensityThreshold;

    /**
     * 壁検出ストリップの幅（画像の幅・高さに対する比率）。
     * 例: 0.2 なら上位 20% 領域を前方ストリップとして使用する。
     */
    private final double stripRatio;

    // 中間バッファ（呼び出しごとの再アロケートを避けるため再利用）
    private final Mat gray = new Mat();
    private final Mat blurred = new Mat();
    private final Mat edges = new Mat();

    /**
     * デフォルトパラメータで初期化する。
     * 実機での調整が必要な場合は {@link #DownwardVisionModule(double, double, double, double)} を使用。
     */
    public DownwardVisionModule() {
        this(50.0, 150.0, 0.05, 0.2);
    }

    /**
     * @param cannyThresholdLow        Canny 低閾値
     * @param cannyThresholdHigh       Canny 高閾値
     * @param wallEdgeDensityThreshold 壁判定エッジ密度閾値（0.0〜1.0）
     * @param stripRatio               検出ストリップの幅比率（0.0〜0.5）
     */
    public DownwardVisionModule(
            double cannyThresholdLow,
            double cannyThresholdHigh,
            double wallEdgeDensityThreshold,
            double stripRatio
    ) {
        this.cannyThresholdLow = cannyThresholdLow;
        this.cannyThresholdHigh = cannyThresholdHigh;
        this.wallEdgeDensityThreshold = wallEdgeDensityThreshold;
        this.stripRatio = stripRatio;
    }

    @Override
    public WallInfo detectWalls(CameraFrame frame) {
        Mat image = frame.image();
        int width = image.cols();
        int height = image.rows();

        // グレースケール変換（入力は BGR）
        cvtColor(image, gray, COLOR_BGR2GRAY);

        // ガウシアンブラーでノイズ除去（カーネルサイズ 5x5）
        GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // Canny エッジ検出
        Canny(blurred, edges, cannyThresholdLow, cannyThresholdHigh);

        int stripW = (int) (width * stripRatio);
        int stripH = (int) (height * stripRatio);

        // 前後左右のストリップでエッジ密度を測定
        boolean front = hasWall(edges, new Rect(0,           0,            width,  stripH));
        boolean back  = hasWall(edges, new Rect(0,           height - stripH, width,  stripH));
        boolean left  = hasWall(edges, new Rect(0,           0,            stripW, height));
        boolean right = hasWall(edges, new Rect(width - stripW, 0,          stripW, height));

        return new WallInfo(front, right, back, left, frame.timestamp());
    }

    /**
     * 指定領域のエッジ密度が閾値を超えるかを判定する。
     *
     * @param edgeImage Canny エッジ画像（グレースケール）
     * @param region    判定対象の矩形領域
     * @return 壁ありと判定した場合 true
     */
    private boolean hasWall(Mat edgeImage, Rect region) {
        try (Mat strip = new Mat(edgeImage, region)) {
            int edgePixels = countNonZero(strip);
            double density = (double) edgePixels / ((long) region.width() * region.height());
            return density >= wallEdgeDensityThreshold;
        }
    }

    @Override
    public void shutdown() {
        gray.release();
        blurred.release();
        edges.release();
    }
}
