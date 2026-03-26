package com.t_horie.omni_mouse.planning.mapping;

/**
 * 迷路のセル壁の状態。
 */
public enum WallState {
    /** 未探索（壁の有無が不明）。 */
    UNKNOWN,
    /** 壁なし（通路）。 */
    OPEN,
    /** 壁あり。 */
    WALL
}
