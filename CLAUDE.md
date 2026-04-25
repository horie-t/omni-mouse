# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OmniMouse is a Raspberry Pi 5-based omnidirectional micro mouse robot for maze competitions. It uses 3 omni wheels driven by L6470 stepper motors, a BNO055 IMU, and cameras for wall detection. The robot maintains East-facing orientation while navigating a standard 16×16 micromouse maze (180mm cells).

## Build & Run Commands

```bash
# Build
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName

# Run main application on Raspberry Pi
./mvnw spring-boot:run

# Run standalone camera capture test
java -Dloader.main=com.t_horie.omni_mouse.hardware.camera.CameraCaptureTest \
  -jar target/omni-mouse-*.jar [cameraIndex] [outputPath]
```

Java 25 is required. The project targets a Raspberry Pi 5 running Raspberry Pi OS (bookworm). Pi4J, libcamera (`rpicam-vid`), and I2C/SPI hardware access are only available on the target device. There are currently no test files — the project relies on on-device testing.

## Architecture

The codebase is a Spring Boot / WebFlux application (Spring Boot 4.0, Pi4J 4.0) with a layered control stack and a maze exploration pipeline.

### Control Stack

```
PathFollower (PIDPathFollower)
    └── StabilizationModule (HeadingStabilizer)
            └── MotionControlModule (OmniMotionModule)
                    └── MotorControlModule (L6470MotorModule)  ← SPI
```

### Maze Exploration Pipeline

```
MazeExplorer  (SENSING → MOVING cycle)
    ├── DownwardCameraModule → DownwardVisionModule  (wall detection via Canny edge)
    ├── WallMappingModule  (relative walls → world-frame map update)
    ├── MazeMap  (16×16 grid, bidirectional wall sync)
    ├── FloodFillExplorationModule  (Adachi method BFS pathfinding)
    └── PIDPathFollower  (navigates to next cell)
```

**`hardware/`** — Hardware abstractions (Pi4J-backed):
- `L6470MotorModule`: 3 stepper motors daisy-chained on SPI bus 0. Commands: forward/reverse/stop/freeRun/getPosition.
- `Bno055IMUModule`: BNO055 9-axis sensor via I2C bus 1 (address 0x28), NDOF fusion mode. Returns absolute heading 0–360° (clockwise).
- `RpicamCameraModule`: Reads MJPEG stream from `rpicam-vid`. Detects JPEG frame boundaries (FF D8 / FF D9) and decodes with OpenCV.
- `DownwardCameraModule`: Downward-facing camera (102° FOV) for wall proximity detection.

**`control/`** — Motion control layers:
- `OmniMotionModule`: Inverse kinematics — converts body-frame (vx, vy, ω) to per-wheel speeds. Wheels at 120° spacing, 50mm center radius, 24mm wheel radius.
- `HeadingStabilizer`: Wraps any `MotionControlModule`; adds PID heading-hold (Kp=1.5, Ki=0.2, Kd=0.3). Deadband: if angular command < 0.05 rad/s, switches to heading-hold mode.
- `PIDPathFollower`: Closed-loop waypoint follower. Transforms world-frame position error into robot body frame, applies PD control (position Kp=1.5, Kd=0.2; heading Kp=2.0, Kd=0.3). Speed limits: 0.5 m/s, 3.0 rad/s.

**`sensing/odometry/`** — Odometry:
- `FusedOdometryModule`: Fuses wheel encoder deltas (forward kinematics pseudoinverse) with BNO055 absolute heading. Runs at 100 Hz. Heading comes from IMU (drift-free); position from encoder displacement rotated into world frame.
- `IMUOdometryModule`: IMU-only fallback; integrates acceleration with velocity decay (0.95). Not recommended for production use.

**`sensing/vision/`** — Vision:
- `DownwardVisionModule`: Detects walls using downward camera via Canny edge detection on image strips. Measures edge density in front/back/left/right regions.

**`planning/mapping/`** — Maze mapping:
- `MazeMap`: Standard 16×16 micromouse maze grid (180mm cells). Manages 4-directional walls per cell with bidirectional sync of adjacent cells. Tracks explored cells; provides world-meter ↔ cell-index conversion.
- `WallMappingModule`: Converts robot-relative wall detections to world coordinates using current pose/heading. Only updates UNKNOWN walls to prevent overwriting confirmed detections.

**`planning/exploration/`** — Exploration:
- `FloodFillExplorationModule`: Flood-fill (Adachi method) via BFS from goal to current cell. Treats UNKNOWN walls as passable for optimistic exploration.

**`MazeExplorer.java`**: Orchestrates the SENSING→MOVING exploration cycle until goal is reached.

**`OmniMouseApplication.java`**: Wires all modules, runs 100 Hz closed-loop control loop via reactive `Flux` (Project Reactor).

## Key Constants & Configuration

| Parameter | Value | Location |
|-----------|-------|----------|
| Maze cell size | 180 mm | `MazeMap` |
| Maze dimensions | 16×16 | `MazeMap` |
| Wheel radius | 24 mm | `OmniMotionModule` |
| Center radius | 50 mm | `OmniMotionModule` |
| Motor KVAL (PWM) | 0x40 (25%) | `OmniMouseApplication` |
| SPI baud | 4 MHz | `L6470MotorModule` |
| IMU I2C address | 0x28 | `Bno055IMUModule` |
| Control loop rate | 100 Hz | `OmniMouseApplication` |
| Camera resolution | 640×480 @ 30fps | `RpicamCameraModule` |
| Downward camera FOV | 102° | `DownwardCameraModule` |

## Module Interaction Diagrams

PlantUML source and rendered SVGs are in `docs/`:
- `docs/data-flow.puml` / `docs/data-flow.svg`
- `docs/module-structure.puml` / `docs/module-structure.svg`
