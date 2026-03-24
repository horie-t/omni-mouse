package com.t_horie.omni_mouse.control.path;

import com.t_horie.omni_mouse.sensing.odometry.Pose;
import com.t_horie.omni_mouse.sensing.odometry.Velocity;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Closed-loop path following controller.
 *
 * <p>Data-flow role:
 * <ul>
 *   <li>{@code PATH → FOLLOW : 目標経路 (座標列)} — call {@link #setPath}</li>
 *   <li>{@code LOCAL → FOLLOW : 現在位置・姿勢} — pass to {@link #computeVelocity}</li>
 *   <li>{@code FOLLOW → MOTION : 目標速度 (Vx, Vy, ω)} — return value</li>
 * </ul>
 *
 * <p>Imperative usage:
 * <pre>{@code
 *   follower.setPath(List.of(wp1, wp2, wp3));
 *   while (!follower.isComplete()) {
 *       Velocity v = follower.computeVelocity(odometry.getCurrentOdometry().pose());
 *       motion.move(v.vx(), v.vy(), v.omega());
 *   }
 * }</pre>
 *
 * <p>Reactive usage:
 * <pre>{@code
 *   follower.setPath(waypoints);
 *   follower.follow(poseFlux)
 *           .takeUntil(_ -> follower.isComplete())
 *           .subscribe(v -> motion.move(v.vx(), v.vy(), v.omega()));
 * }</pre>
 */
public interface PathFollowingModule {

    /**
     * Set the path to follow.  Can be called at any time to replace the current path.
     *
     * @param waypoints ordered list of target poses; last pose includes heading goal
     */
    void setPath(List<Pose> waypoints);

    /**
     * Compute the velocity command that moves the robot toward the current waypoint.
     * Advances to the next waypoint automatically when within tolerance.
     *
     * @param currentPose current robot pose (from OdometryModule / LocalizationModule)
     * @return body-frame velocity command; {@link Velocity#zero()} when path is complete
     */
    Velocity computeVelocity(Pose currentPose);

    /** @return true when the last waypoint has been reached */
    boolean isComplete();

    /** Clear path and reset controller state. */
    void reset();

    /**
     * Reactive wrapper: maps each pose update to a velocity command.
     * The returned Flux emits {@link Velocity#zero()} after {@link #isComplete()} becomes true.
     */
    default Flux<Velocity> follow(Flux<Pose> poseStream) {
        return poseStream.map(this::computeVelocity);
    }
}
