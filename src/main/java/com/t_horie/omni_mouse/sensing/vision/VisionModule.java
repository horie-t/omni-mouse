package com.t_horie.omni_mouse.sensing.vision;

import com.t_horie.omni_mouse.hardware.camera.CameraFrame;

/**
 * 画像処理による壁検出モジュールのインターフェース。
 *
 * <p>実装クラスはカメラフレームを受け取り、ロボット周囲4方向の壁の有無を返す。
 * フレームのライフサイクル（release()）は呼び出し元が管理する。
 */
public interface VisionModule {

    /**
     * カメラフレームからロボット周囲4方向の壁を検出する。
     *
     * @param frame カメラフレーム（呼び出し元が release() を管理すること）
     * @return 壁検出結果
     */
    WallInfo detectWalls(CameraFrame frame);

    /**
     * リソースを解放する。以降 detectWalls() を呼び出してはならない。
     */
    void shutdown();
}
