package com.t_horie.omni_mouse.planning.mapping;

import com.t_horie.omni_mouse.sensing.odometry.Pose;
import com.t_horie.omni_mouse.sensing.vision.WallInfo;

/**
 * 迷路マッピングモジュールのインターフェース。
 *
 * <p>オドメトリとビジョンの情報を受け取り、{@link MazeMap} を更新する。
 * ExplorationModule と PathPlanningModule は {@link #getMap()} でマップを参照する。
 */
public interface MappingModule {

    /**
     * 現在の姿勢と壁検出情報でマップを更新する。
     * 制御ループ（100Hz または 30Hz）から呼び出す。
     *
     * @param pose     現在のロボット姿勢（世界座標系）
     * @param wallInfo ロボット座標系の壁検出結果
     */
    void update(Pose pose, WallInfo wallInfo);

    /**
     * 現在の迷路マップを返す。
     * 返されたオブジェクトへの変更は呼び出し元からは行わないこと。
     *
     * @return 迷路マップ
     */
    MazeMap getMap();

    /**
     * マップと探索状態を初期化する。
     */
    void reset();
}
