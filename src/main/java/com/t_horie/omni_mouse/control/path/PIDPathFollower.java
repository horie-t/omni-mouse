package com.t_horie.omni_mouse.control.path;

import com.t_horie.omni_mouse.sensing.odometry.Pose;
import com.t_horie.omni_mouse.sensing.odometry.Velocity;

import java.util.ArrayList;
import java.util.List;

/**
 * PD closed-loop path follower for the omnidirectional robot.
 *
 * <h2>Controller structure</h2>
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  World-frame error  →  robot body-frame  →  PD → clamp     │
 *   │                                                             │
 *   │  ex_b =  (tx−cx)·cosθ + (ty−cy)·sinθ                      │
 *   │  ey_b = −(tx−cx)·sinθ + (ty−cy)·cosθ                      │
 *   │                                                             │
 *   │  Vx = Kp·ex_b + Kd·ėx_b                                   │
 *   │  Vy = Kp·ey_b + Kd·ėy_b                                   │
 *   │  ω  = Kp_ang·eθ + Kd_ang·ėθ   (last waypoint only)        │
 *   └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Waypoint advancement</h2>
 * <ul>
 *   <li>Intermediate waypoints: position tolerance only</li>
 *   <li>Final waypoint: position AND heading tolerance</li>
 * </ul>
 *
 * <h2>Default gains / limits</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>Kp_lin</td><td>1.5</td><td>position P gain (m/s per m)</td></tr>
 *   <tr><td>Kd_lin</td><td>0.2</td><td>position D gain (m/s per m/s)</td></tr>
 *   <tr><td>Kp_ang</td><td>2.0</td><td>heading P gain (rad/s per rad)</td></tr>
 *   <tr><td>Kd_ang</td><td>0.3</td><td>heading D gain (rad/s per rad/s)</td></tr>
 *   <tr><td>maxLinSpeed</td><td>0.5 m/s</td><td>linear speed clamp</td></tr>
 *   <tr><td>maxAngSpeed</td><td>3.0 rad/s</td><td>angular speed clamp</td></tr>
 *   <tr><td>posTolerance</td><td>0.02 m</td><td>waypoint reached distance</td></tr>
 *   <tr><td>angTolerance</td><td>0.05 rad (~3°)</td><td>final heading tolerance</td></tr>
 * </table>
 */
public class PIDPathFollower implements PathFollowingModule {

    // --- Default tuning ---
    private static final double DEFAULT_KP_LIN       = 1.5;
    private static final double DEFAULT_KD_LIN       = 0.2;
    private static final double DEFAULT_KP_ANG       = 2.0;
    private static final double DEFAULT_KD_ANG       = 0.3;
    private static final double DEFAULT_MAX_LIN_SPEED = 0.5;   // m/s
    private static final double DEFAULT_MAX_ANG_SPEED = 3.0;   // rad/s
    private static final double DEFAULT_POS_TOLERANCE = 0.02;  // m
    private static final double DEFAULT_ANG_TOLERANCE = 0.05;  // rad

    // PD gains
    private final double kpLin;
    private final double kdLin;
    private final double kpAng;
    private final double kdAng;

    // Speed limits
    private final double maxLinSpeed;
    private final double maxAngSpeed;

    // Waypoint tolerances
    private final double posTolerance;
    private final double angTolerance;

    // Path state — guarded by 'this'
    private final List<Pose> waypoints = new ArrayList<>();
    private int waypointIdx;

    // Derivative state
    private double prevErrX;     // body-frame x error at last update
    private double prevErrY;     // body-frame y error at last update
    private double prevErrTheta; // heading error at last update
    private long   prevUpdateNs;

    /** Construct with default tuning parameters. */
    public PIDPathFollower() {
        this(DEFAULT_KP_LIN, DEFAULT_KD_LIN,
             DEFAULT_KP_ANG, DEFAULT_KD_ANG,
             DEFAULT_MAX_LIN_SPEED, DEFAULT_MAX_ANG_SPEED,
             DEFAULT_POS_TOLERANCE, DEFAULT_ANG_TOLERANCE);
    }

    /**
     * Construct with custom tuning.
     *
     * @param kpLin       position proportional gain (m/s per m)
     * @param kdLin       position derivative gain   (m/s per m/s)
     * @param kpAng       heading proportional gain  (rad/s per rad)
     * @param kdAng       heading derivative gain    (rad/s per rad/s)
     * @param maxLinSpeed max linear speed (m/s)
     * @param maxAngSpeed max angular speed (rad/s)
     * @param posTolerance waypoint-reached distance (m)
     * @param angTolerance final-waypoint heading tolerance (rad)
     */
    public PIDPathFollower(double kpLin, double kdLin,
                           double kpAng, double kdAng,
                           double maxLinSpeed, double maxAngSpeed,
                           double posTolerance, double angTolerance) {
        this.kpLin       = kpLin;
        this.kdLin       = kdLin;
        this.kpAng       = kpAng;
        this.kdAng       = kdAng;
        this.maxLinSpeed = maxLinSpeed;
        this.maxAngSpeed = maxAngSpeed;
        this.posTolerance = posTolerance;
        this.angTolerance = angTolerance;
    }

    // -------------------------------------------------------------------------
    // PathFollowingModule interface
    // -------------------------------------------------------------------------

    @Override
    public synchronized void setPath(List<Pose> newWaypoints) {
        waypoints.clear();
        waypoints.addAll(newWaypoints);
        waypointIdx = 0;
        resetDerivativeState();
    }

    @Override
    public synchronized Velocity computeVelocity(Pose current) {
        if (isComplete()) return Velocity.zero();

        long   now = System.nanoTime();
        double dt  = Math.min((now - prevUpdateNs) * 1e-9, 0.1);
        if (dt <= 0) dt = 0.01;
        prevUpdateNs = now;

        // Advance past any already-reached waypoints
        while (!isComplete() && isWaypointReached(current, waypoints.get(waypointIdx))) {
            waypointIdx++;
            resetDerivativeState();
            if (isComplete()) return Velocity.zero();
        }

        Pose   target  = waypoints.get(waypointIdx);
        boolean isLast = (waypointIdx == waypoints.size() - 1);

        // --- Position error in world frame ---
        double ex_w = target.x() - current.x();
        double ey_w = target.y() - current.y();

        // --- Transform to robot body frame (rotate by −θ) ---
        double cosT = Math.cos(current.theta());
        double sinT = Math.sin(current.theta());
        double ex_b =  ex_w * cosT + ey_w * sinT;
        double ey_b = -ex_w * sinT + ey_w * cosT;

        // --- Heading error (enforced only at the final waypoint) ---
        double eTheta = isLast
                ? normalizeAngle(target.theta() - current.theta())
                : 0.0;

        // --- PD: derivatives from previous errors ---
        double dex_b  = (ex_b  - prevErrX)     / dt;
        double dey_b  = (ey_b  - prevErrY)     / dt;
        double deTheta = (eTheta - prevErrTheta) / dt;

        prevErrX     = ex_b;
        prevErrY     = ey_b;
        prevErrTheta = eTheta;

        // --- PD control output ---
        double vx    = clamp(kpLin * ex_b   + kdLin * dex_b,  -maxLinSpeed, maxLinSpeed);
        double vy    = clamp(kpLin * ey_b   + kdLin * dey_b,  -maxLinSpeed, maxLinSpeed);
        double omega = clamp(kpAng * eTheta + kdAng * deTheta, -maxAngSpeed, maxAngSpeed);

        return new Velocity(vx, vy, omega);
    }

    @Override
    public synchronized boolean isComplete() {
        return waypoints.isEmpty() || waypointIdx >= waypoints.size();
    }

    @Override
    public synchronized void reset() {
        waypoints.clear();
        waypointIdx = 0;
        resetDerivativeState();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * True when {@code current} is within tolerance of {@code target}.
     * For intermediate waypoints only position is checked.
     * For the final waypoint both position and heading must be satisfied.
     */
    private boolean isWaypointReached(Pose current, Pose target) {
        double dist = Math.hypot(target.x() - current.x(), target.y() - current.y());
        if (dist >= posTolerance) return false;

        boolean isLast = waypoints.indexOf(target) == waypoints.size() - 1;
        if (!isLast) return true; // intermediate: position only

        double dTheta = Math.abs(normalizeAngle(target.theta() - current.theta()));
        return dTheta < angTolerance;
    }

    private void resetDerivativeState() {
        prevErrX     = 0;
        prevErrY     = 0;
        prevErrTheta = 0;
        prevUpdateNs = System.nanoTime();
    }

    /** Normalize angle to (−π, +π]. */
    private static double normalizeAngle(double angle) {
        while (angle >  Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
