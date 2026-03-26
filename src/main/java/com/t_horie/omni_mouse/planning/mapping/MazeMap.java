package com.t_horie.omni_mouse.planning.mapping;

/**
 * マイクロマウス標準規格の 16×16 セル迷路マップ。
 *
 * <p>座標系:
 * <ul>
 *   <li>セル (col=0, row=0) = スタート位置（南西隅）</li>
 *   <li>col（列）は East 方向に増加</li>
 *   <li>row（行）は North 方向に増加</li>
 * </ul>
 *
 * <p>壁は隣接セル間で共有される。{@link #setWall} は自動的に対面セルへも反映する。
 */
public class MazeMap {

    /** 迷路サイズ（一辺のセル数）。 */
    public static final int SIZE = 16;

    /** 1 セルの物理サイズ（メートル）。マイクロマウス標準規格 180mm。 */
    public static final double CELL_SIZE_M = 0.18;

    // walls[row][col][direction.ordinal()] で壁状態を管理
    private final WallState[][][] walls = new WallState[SIZE][SIZE][4];
    private final boolean[][] explored = new boolean[SIZE][SIZE];

    public MazeMap() {
        reset();
    }

    /**
     * マップを初期状態に戻す。外周壁のみ WALL、内部はすべて UNKNOWN。
     */
    public void reset() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                for (int d = 0; d < 4; d++) {
                    walls[row][col][d] = WallState.UNKNOWN;
                }
                explored[row][col] = false;
            }
        }
        initOuterWalls();
    }

    private void initOuterWalls() {
        for (int i = 0; i < SIZE; i++) {
            walls[0][i][Direction.SOUTH.ordinal()]        = WallState.WALL;
            walls[SIZE - 1][i][Direction.NORTH.ordinal()] = WallState.WALL;
            walls[i][0][Direction.WEST.ordinal()]         = WallState.WALL;
            walls[i][SIZE - 1][Direction.EAST.ordinal()]  = WallState.WALL;
        }
    }

    /**
     * 指定セルの指定方向の壁状態を設定する。
     * 隣接セルの対面壁にも同じ状態を自動反映する。
     *
     * @param col セル列インデックス [0, SIZE)
     * @param row セル行インデックス [0, SIZE)
     * @param dir 壁の方向
     * @param state 設定する壁状態
     */
    public void setWall(int col, int row, Direction dir, WallState state) {
        walls[row][col][dir.ordinal()] = state;
        int nCol = col + dir.dx;
        int nRow = row + dir.dy;
        if (inBounds(nCol, nRow)) {
            walls[nRow][nCol][dir.opposite().ordinal()] = state;
        }
    }

    /**
     * 指定セルの指定方向の壁状態を返す。
     *
     * @param col セル列インデックス [0, SIZE)
     * @param row セル行インデックス [0, SIZE)
     * @param dir 壁の方向
     * @return 壁状態
     */
    public WallState getWall(int col, int row, Direction dir) {
        return walls[row][col][dir.ordinal()];
    }

    /**
     * 指定セルを探索済みとしてマークする。
     */
    public void markExplored(int col, int row) {
        explored[row][col] = true;
    }

    /**
     * 指定セルが探索済みかを返す。
     */
    public boolean isExplored(int col, int row) {
        return explored[row][col];
    }

    /**
     * 指定のセル座標が迷路範囲内かを返す。
     */
    public boolean inBounds(int col, int row) {
        return col >= 0 && col < SIZE && row >= 0 && row < SIZE;
    }

    /**
     * 世界座標 X（メートル）からセル列インデックスを返す。
     */
    public static int toCol(double worldX) {
        return Math.clamp((int) (worldX / CELL_SIZE_M), 0, SIZE - 1);
    }

    /**
     * 世界座標 Y（メートル）からセル行インデックスを返す。
     */
    public static int toRow(double worldY) {
        return Math.clamp((int) (worldY / CELL_SIZE_M), 0, SIZE - 1);
    }

    /**
     * セル列インデックスから、そのセル中心の世界座標 X（メートル）を返す。
     */
    public static double cellCenterX(int col) {
        return (col + 0.5) * CELL_SIZE_M;
    }

    /**
     * セル行インデックスから、そのセル中心の世界座標 Y（メートル）を返す。
     */
    public static double cellCenterY(int row) {
        return (row + 0.5) * CELL_SIZE_M;
    }

    /**
     * デバッグ用: マップをテキストで表示する。
     * 壁は '+', '-', '|' で、未探索は '?' で表す。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = SIZE - 1; row >= 0; row--) {
            // 北壁
            sb.append('+');
            for (int col = 0; col < SIZE; col++) {
                sb.append(walls[row][col][Direction.NORTH.ordinal()] == WallState.WALL ? "---" : "   ");
                sb.append('+');
            }
            sb.append('\n');
            // 東西壁とセル内容
            sb.append(walls[row][0][Direction.WEST.ordinal()] == WallState.WALL ? '|' : ' ');
            for (int col = 0; col < SIZE; col++) {
                sb.append(explored[row][col] ? "   " : " ? ");
                sb.append(walls[row][col][Direction.EAST.ordinal()] == WallState.WALL ? '|' : ' ');
            }
            sb.append('\n');
        }
        // 最下行の南壁
        sb.append('+');
        for (int col = 0; col < SIZE; col++) {
            sb.append(walls[0][col][Direction.SOUTH.ordinal()] == WallState.WALL ? "---" : "   ");
            sb.append('+');
        }
        return sb.toString();
    }
}
