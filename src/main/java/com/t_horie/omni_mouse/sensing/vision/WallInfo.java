package com.t_horie.omni_mouse.sensing.vision;

import java.time.Instant;

/**
 * ロボット周囲4方向の壁検出結果。
 * 方向はロボット座標系（ロボット前方を基準）。
 */
public record WallInfo(
        boolean front,
        boolean right,
        boolean back,
        boolean left,
        Instant timestamp
) {
    public static WallInfo none(Instant timestamp) {
        return new WallInfo(false, false, false, false, timestamp);
    }

    @Override
    public String toString() {
        return "WallInfo{F=%b R=%b B=%b L=%b at %s}".formatted(front, right, back, left, timestamp);
    }
}
