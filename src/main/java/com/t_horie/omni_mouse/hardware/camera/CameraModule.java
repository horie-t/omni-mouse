package com.t_horie.omni_mouse.hardware.camera;

/**
 * カメラモジュールのハードウェア抽象化インターフェース。
 * start() でストリームを開始し、captureFrame() でフレームを取得する。
 */
public interface CameraModule {

    /**
     * カメラストリームを開始する。
     * captureFrame() を呼ぶ前に一度呼び出すこと。
     *
     * @throws RuntimeException 起動に失敗した場合
     */
    void start();

    /**
     * 次のフレームを取得する（ブロッキング呼び出し）。
     * 返された CameraFrame の release() を呼び出してリソースを解放すること。
     *
     * @return 取得したフレーム
     * @throws RuntimeException フレーム取得に失敗した場合
     */
    CameraFrame captureFrame();

    /**
     * カメラストリームを停止しリソースを解放する。
     */
    void shutdown();
}
