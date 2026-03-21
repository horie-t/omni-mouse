package com.t_horie.omni_mouse.sensing.odometry;

import reactor.core.publisher.Flux;

/**
 * Odometry module for estimating robot position and velocity.
 * Provides reactive stream of odometry data at approximately 100Hz.
 */
public interface OdometryModule {
    /**
     * Start odometry estimation and return a stream of odometry data.
     *
     * @return Flux stream of odometry data
     */
    Flux<OdometryData> start();

    /**
     * Reset odometry to origin (0, 0, 0).
     */
    void reset();

    /**
     * Get current odometry data.
     *
     * @return current odometry data
     */
    OdometryData getCurrentOdometry();

    /**
     * Shutdown the odometry module.
     */
    void shutdown();
}
