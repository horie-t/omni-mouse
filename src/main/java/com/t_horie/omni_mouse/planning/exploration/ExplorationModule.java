package com.t_horie.omni_mouse.planning.exploration;

import com.t_horie.omni_mouse.planning.mapping.CellCoord;
import com.t_horie.omni_mouse.planning.mapping.MazeMap;

import java.util.Optional;

/**
 * 迷路探索モジュールのインターフェース。
 *
 * <p>足立法（フラッド・フィル）を基本とし、現在のマップ情報から
 * ゴールへの最短距離方向に次のセルを返す。
 *
 * <p>使用例:
 * <pre>{@code
 * exploration.recompute(map);             // 壁情報更新後に距離マップを再計算
 * Optional<CellCoord> next =
 *     exploration.computeNextCell(col, row, map);  // 次のセルを取得
 * }</pre>
 */
public interface ExplorationModule {

    /**
     * 現在位置から次に向かうべきセルを返す。
     *
     * <p>UNKNOWN 壁は通過可能（楽観的探索）として扱う。
     * 現在位置がゴールセルの場合は {@link Optional#empty()} を返す。
     *
     * @param currentCol 現在のセル列インデックス
     * @param currentRow 現在のセル行インデックス
     * @param map        現在の迷路マップ
     * @return 次の目標セル、ゴール到達済みまたは到達不能の場合は empty
     */
    Optional<CellCoord> computeNextCell(int currentCol, int currentRow, MazeMap map);

    /**
     * 距離マップをフラッド・フィルで再計算する。
     * マップの壁情報が更新されるたびに呼び出すこと。
     *
     * @param map 最新の迷路マップ
     */
    void recompute(MazeMap map);

    /**
     * 指定セルがゴールセルかを返す。
     *
     * @param col セル列インデックス
     * @param row セル行インデックス
     * @return ゴールセルなら true
     */
    boolean isGoalCell(int col, int row);

    /**
     * 距離マップを返す（デバッグ・PathPlanning 連携用）。
     * 値は各セルからゴールまでの推定距離。到達不能セルは {@link Integer#MAX_VALUE}。
     *
     * @return distances[row][col] の形式の距離マップのコピー
     */
    int[][] getDistances();

    /**
     * 探索状態と距離マップをリセットする。
     */
    void reset();
}
