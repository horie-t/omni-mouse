package com.t_horie.omni_mouse.planning.mapping;

/**
 * 迷路内の4方向。世界座標系（x=East, y=North）を基準とする。
 * ordinal は NORTH=0, EAST=1, SOUTH=2, WEST=3 の順。
 */
public enum Direction {
    NORTH(0, 1),
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0);

    /** X 方向の増分（East が正）。 */
    public final int dx;
    /** Y 方向の増分（North が正）。 */
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** 正反対の方向を返す（NORTH ↔ SOUTH, EAST ↔ WEST）。 */
    public Direction opposite() {
        return values()[(ordinal() + 2) % 4];
    }

    /** 時計回りに90°回転した方向を返す（NORTH → EAST → SOUTH → WEST）。 */
    public Direction rotateClockwise() {
        return values()[(ordinal() + 1) % 4];
    }

    /** 反時計回りに90°回転した方向を返す（NORTH → WEST → SOUTH → EAST）。 */
    public Direction rotateCounterClockwise() {
        return values()[(ordinal() + 3) % 4];
    }

    /**
     * ロボットの向き（CCW ラジアン、0=East）を最近傍の4方向に変換する。
     *
     * @param theta ロボットの向き（ラジアン）
     * @return 最も近い方角
     */
    public static Direction fromHeading(double theta) {
        // [0, 2π) に正規化
        double normalized = ((theta % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        // π/2 単位で量子化: 0=EAST, 1=NORTH, 2=WEST, 3=SOUTH
        int sector = (int) Math.round(normalized / (Math.PI / 2)) % 4;
        return switch (sector) {
            case 0 -> EAST;
            case 1 -> NORTH;
            case 2 -> WEST;
            case 3 -> SOUTH;
            default -> EAST;
        };
    }
}
