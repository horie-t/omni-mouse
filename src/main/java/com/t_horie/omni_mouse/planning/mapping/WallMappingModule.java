package com.t_horie.omni_mouse.planning.mapping;

import com.t_horie.omni_mouse.sensing.odometry.Pose;
import com.t_horie.omni_mouse.sensing.vision.WallInfo;

/**
 * 壁検出情報とオドメトリを使った迷路マッピングモジュール。
 *
 * <p>処理フロー:
 * <ol>
 *   <li>現在姿勢からセル座標を算出する</li>
 *   <li>ロボットの向きを4方向に量子化する</li>
 *   <li>ロボット座標系の壁情報（前後左右）を世界座標系（N/E/S/W）に変換する</li>
 *   <li>未確定（UNKNOWN）の壁のみ更新し、確定済み壁は保持する</li>
 *   <li>現在セルを探索済みにマークする</li>
 * </ol>
 *
 * <p>スレッドセーフではない。単一スレッドから呼び出すこと。
 */
public class WallMappingModule implements MappingModule {

    private final MazeMap map = new MazeMap();

    @Override
    public void update(Pose pose, WallInfo wallInfo) {
        int col = MazeMap.toCol(pose.x());
        int row = MazeMap.toRow(pose.y());

        // ロボットの向きを世界4方向に量子化
        Direction facing = Direction.fromHeading(pose.theta());

        // ロボット座標系 → 世界座標系の壁を更新
        // front = 向いている方向, right = 時計回り90°, back = 反対, left = 反時計回り90°
        applyWall(col, row, facing,                          wallInfo.front());
        applyWall(col, row, facing.rotateClockwise(),        wallInfo.right());
        applyWall(col, row, facing.opposite(),               wallInfo.back());
        applyWall(col, row, facing.rotateCounterClockwise(), wallInfo.left());

        map.markExplored(col, row);
    }

    /**
     * 指定方向の壁状態を更新する。
     * 既に WALL または OPEN が確定している場合は変更しない（誤検知による上書きを防ぐ）。
     */
    private void applyWall(int col, int row, Direction dir, boolean hasWall) {
        if (map.getWall(col, row, dir) == WallState.UNKNOWN) {
            map.setWall(col, row, dir, hasWall ? WallState.WALL : WallState.OPEN);
        }
    }

    @Override
    public MazeMap getMap() {
        return map;
    }

    @Override
    public void reset() {
        map.reset();
    }
}
