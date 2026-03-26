package com.t_horie.omni_mouse.planning.mapping;

/**
 * 迷路内のセル座標。col（列=East方向）と row（行=North方向）で表す。
 */
public record CellCoord(int col, int row) {

    /**
     * 指定方向に隣接するセル座標を返す。
     */
    public CellCoord neighbor(Direction dir) {
        return new CellCoord(col + dir.dx, row + dir.dy);
    }

    @Override
    public String toString() {
        return "(%d, %d)".formatted(col, row);
    }
}
