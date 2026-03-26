package com.t_horie.omni_mouse.planning.exploration;

import com.t_horie.omni_mouse.planning.mapping.CellCoord;
import com.t_horie.omni_mouse.planning.mapping.Direction;
import com.t_horie.omni_mouse.planning.mapping.MazeMap;
import com.t_horie.omni_mouse.planning.mapping.WallState;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * 足立法（フラッド・フィル）による迷路探索モジュール。
 *
 * <p>アルゴリズム概要:
 * <ol>
 *   <li>ゴールセルを距離 0 としてBFSを開始する</li>
 *   <li>UNKNOWN 壁は通過可能（楽観的）として距離を伝播する</li>
 *   <li>ロボットは常に最小距離の隣接セルへ移動する</li>
 *   <li>新しい壁が発見されたら {@link #recompute} で距離マップを更新する</li>
 * </ol>
 *
 * <p>ゴールセル: デフォルトはマイクロマウス標準規格の中央4セル (7,7)(7,8)(8,7)(8,8)。
 * コンストラクタでカスタムゴールを指定可能。
 */
public class FloodFillExplorationModule implements ExplorationModule {

    private static final int UNREACHABLE = Integer.MAX_VALUE;

    private final boolean[][] isGoal;
    private final int[][] distances;

    /**
     * 標準ゴール（中央4セル）で初期化する。
     */
    public FloodFillExplorationModule() {
        this(Set.of(
                new CellCoord(7, 7), new CellCoord(8, 7),
                new CellCoord(7, 8), new CellCoord(8, 8)
        ));
    }

    /**
     * カスタムゴールセルで初期化する。
     *
     * @param goalCells ゴールとするセル座標の集合
     */
    public FloodFillExplorationModule(Set<CellCoord> goalCells) {
        isGoal = new boolean[MazeMap.SIZE][MazeMap.SIZE];
        distances = new int[MazeMap.SIZE][MazeMap.SIZE];
        for (CellCoord c : goalCells) {
            isGoal[c.row()][c.col()] = true;
        }
        reset();
    }

    /**
     * {@inheritDoc}
     *
     * <p>UNKNOWN 壁は通過可能として扱う（楽観的探索）。
     * これにより未探索エリアへの進入を許容し、探索を効率化する。
     */
    @Override
    public void recompute(MazeMap map) {
        for (int[] row : distances) Arrays.fill(row, UNREACHABLE);

        Queue<CellCoord> queue = new ArrayDeque<>();

        // ゴールセルを距離 0 でキューに積む
        for (int row = 0; row < MazeMap.SIZE; row++) {
            for (int col = 0; col < MazeMap.SIZE; col++) {
                if (isGoal[row][col]) {
                    distances[row][col] = 0;
                    queue.add(new CellCoord(col, row));
                }
            }
        }

        // BFS で距離を伝播
        while (!queue.isEmpty()) {
            CellCoord curr = queue.poll();
            int nextDist = distances[curr.row()][curr.col()] + 1;

            for (Direction dir : Direction.values()) {
                // WALL のみブロック、UNKNOWN は通過可能とみなす
                if (map.getWall(curr.col(), curr.row(), dir) == WallState.WALL) continue;

                CellCoord neighbor = curr.neighbor(dir);
                if (!map.inBounds(neighbor.col(), neighbor.row())) continue;
                if (distances[neighbor.row()][neighbor.col()] != UNREACHABLE) continue;

                distances[neighbor.row()][neighbor.col()] = nextDist;
                queue.add(neighbor);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>現在セルの隣接セルのうち、壁が WALL でなく（OPEN または UNKNOWN）、
     * 距離マップ上で最小値を持つセルを返す。
     */
    @Override
    public Optional<CellCoord> computeNextCell(int currentCol, int currentRow, MazeMap map) {
        if (isGoalCell(currentCol, currentRow)) return Optional.empty();

        CellCoord best = null;
        int bestDist = UNREACHABLE;

        for (Direction dir : Direction.values()) {
            if (map.getWall(currentCol, currentRow, dir) == WallState.WALL) continue;

            CellCoord neighbor = new CellCoord(currentCol + dir.dx, currentRow + dir.dy);
            if (!map.inBounds(neighbor.col(), neighbor.row())) continue;

            int d = distances[neighbor.row()][neighbor.col()];
            if (d < bestDist) {
                bestDist = d;
                best = neighbor;
            }
        }

        return Optional.ofNullable(best);
    }

    @Override
    public boolean isGoalCell(int col, int row) {
        if (col < 0 || col >= MazeMap.SIZE || row < 0 || row >= MazeMap.SIZE) return false;
        return isGoal[row][col];
    }

    @Override
    public int[][] getDistances() {
        int[][] copy = new int[MazeMap.SIZE][MazeMap.SIZE];
        for (int i = 0; i < MazeMap.SIZE; i++) {
            copy[i] = Arrays.copyOf(distances[i], MazeMap.SIZE);
        }
        return copy;
    }

    @Override
    public void reset() {
        for (int[] row : distances) Arrays.fill(row, UNREACHABLE);
    }
}
