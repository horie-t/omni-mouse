package com.t_horie.omni_mouse;

import com.t_horie.omni_mouse.control.motion.MotionControlModule;
import com.t_horie.omni_mouse.control.path.PathFollowingModule;
import com.t_horie.omni_mouse.hardware.camera.CameraFrame;
import com.t_horie.omni_mouse.hardware.camera.DownwardCameraModule;
import com.t_horie.omni_mouse.planning.exploration.ExplorationModule;
import com.t_horie.omni_mouse.planning.mapping.CellCoord;
import com.t_horie.omni_mouse.planning.mapping.MazeMap;
import com.t_horie.omni_mouse.planning.mapping.MappingModule;
import com.t_horie.omni_mouse.sensing.odometry.FusedOdometryModule;
import com.t_horie.omni_mouse.sensing.odometry.Pose;
import com.t_horie.omni_mouse.sensing.odometry.Velocity;
import com.t_horie.omni_mouse.sensing.vision.VisionModule;
import com.t_horie.omni_mouse.sensing.vision.WallInfo;

import java.util.List;
import java.util.Optional;

/**
 * 迷路探索ランナー。
 *
 * <p>以下のサイクルをゴール到達まで繰り返す:
 * <ol>
 *   <li><b>SENSING</b>: カメラで壁を検出し、マップを更新、次のセルを決定</li>
 *   <li><b>MOVING</b>: PID パス追従で次のセル中心へ移動</li>
 * </ol>
 *
 * <p>ロボットは常に East（θ=0）を向いたまま並進する。
 * オムニホイールの全方向移動性を活かし、回転なしで任意方向へ移動する。
 */
public class MazeExplorer {

    private static final long CONTROL_PERIOD_MS = 10; // 100 Hz

    private final DownwardCameraModule camera;
    private final VisionModule vision;
    private final MappingModule mapping;
    private final ExplorationModule exploration;
    private final PathFollowingModule follower;
    private final MotionControlModule motion;
    private final FusedOdometryModule odometry;

    private enum State { SENSING, MOVING, DONE }

    public MazeExplorer(
            DownwardCameraModule camera,
            VisionModule vision,
            MappingModule mapping,
            ExplorationModule exploration,
            PathFollowingModule follower,
            MotionControlModule motion,
            FusedOdometryModule odometry
    ) {
        this.camera    = camera;
        this.vision    = vision;
        this.mapping   = mapping;
        this.exploration = exploration;
        this.follower  = follower;
        this.motion    = motion;
        this.odometry  = odometry;
    }

    /**
     * 探索ループを実行する。ゴール到達またはスレッド割り込みまでブロックする。
     *
     * @throws InterruptedException スレッド割り込み時
     */
    public void run() throws InterruptedException {
        camera.start();
        exploration.recompute(mapping.getMap());

        State state = State.SENSING;

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Pose pose = odometry.getCurrentOdometry().pose();
                int col = MazeMap.toCol(pose.x());
                int row = MazeMap.toRow(pose.y());

                state = switch (state) {
                    case SENSING -> sense(pose, col, row);
                    case MOVING  -> move(pose);
                    case DONE    -> State.DONE;
                };

                if (state == State.DONE) break;
                Thread.sleep(CONTROL_PERIOD_MS);
            }
        } finally {
            motion.stop();
            camera.shutdown();
            vision.shutdown();
        }
    }

    // -------------------------------------------------------------------------
    // State handlers
    // -------------------------------------------------------------------------

    /**
     * 壁検出・マップ更新・次セル決定を行い、次の状態を返す。
     */
    private State sense(Pose pose, int col, int row) {
        // 壁検出（カメラフレーム取得はブロッキング）
        CameraFrame frame = camera.captureFrame();
        WallInfo walls = vision.detectWalls(frame);
        frame.release();

        System.out.printf("[SENSE] cell=(%d,%d)  %s%n", col, row, walls);

        // マップ更新 → 足立法で距離マップ再計算
        mapping.update(pose, walls);
        exploration.recompute(mapping.getMap());

        // ゴール判定
        if (exploration.isGoalCell(col, row)) {
            System.out.printf("[GOAL]  cell=(%d,%d) 到達!%n", col, row);
            System.out.println(mapping.getMap());
            return State.DONE;
        }

        // 次セルを取得してパスを設定
        Optional<CellCoord> next = exploration.computeNextCell(col, row, mapping.getMap());
        if (next.isEmpty()) {
            System.out.println("[ERROR] ゴールへの経路が見つかりません。");
            return State.DONE;
        }

        CellCoord nextCell = next.get();
        double targetX = MazeMap.cellCenterX(nextCell.col());
        double targetY = MazeMap.cellCenterY(nextCell.row());

        // 目標: 次セル中心、常に East（θ=0）向きを維持
        follower.setPath(List.of(new Pose(targetX, targetY, 0.0)));

        System.out.printf("[MOVE]  target=(%d,%d) → world=(%.3f, %.3f m)%n",
                nextCell.col(), nextCell.row(), targetX, targetY);
        return State.MOVING;
    }

    /**
     * パス追従制御コマンドを発行し、到達したら SENSING に戻る。
     */
    private State move(Pose pose) {
        Velocity cmd = follower.computeVelocity(pose);
        motion.move(cmd.vx(), cmd.vy(), cmd.omega());

        if (follower.isComplete()) {
            motion.stop();
            return State.SENSING;
        }
        return State.MOVING;
    }
}
