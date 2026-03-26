package com.t_horie.omni_mouse;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiBus;
import com.t_horie.omni_mouse.control.motion.MotionControlModule;
import com.t_horie.omni_mouse.control.motion.OmniMotionModule;
import com.t_horie.omni_mouse.control.path.PIDPathFollower;
import com.t_horie.omni_mouse.control.path.PathFollowingModule;
import com.t_horie.omni_mouse.control.stabilization.HeadingStabilizer;
import com.t_horie.omni_mouse.hardware.camera.RpicamCameraModule;
import com.t_horie.omni_mouse.hardware.imu.Bno055IMUModule;
import com.t_horie.omni_mouse.hardware.motor.L6470MotorModule;
import com.t_horie.omni_mouse.planning.exploration.FloodFillExplorationModule;
import com.t_horie.omni_mouse.planning.mapping.CellCoord;
import com.t_horie.omni_mouse.planning.mapping.WallMappingModule;
import com.t_horie.omni_mouse.sensing.odometry.FusedOdometryModule;
import com.t_horie.omni_mouse.sensing.vision.DownwardVisionModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Set;

@SpringBootApplication
public class OmniMouseApplication {

	private static final SpiBus        SPI_BUS    = SpiBus.BUS_0;
	private static final SpiChipSelect SPI_CS     = SpiChipSelect.CS_0;
	private static final byte          KVAL       = 0x40;
	private static final int           I2C_BUS    = 1;
	private static final int           CAMERA_IDX = 0;

	// 4x4 迷路のゴールセル（右上隅）
	private static final CellCoord GOAL = new CellCoord(3, 3);

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			Context pi4j = Pi4J.newAutoContext();

			// ── Hardware layer ──────────────────────────────────────────────
			var motors = new L6470MotorModule(pi4j, SPI_BUS, SPI_CS, KVAL);
			var imu    = new Bno055IMUModule(pi4j, I2C_BUS);

			// ── Control layer ───────────────────────────────────────────────
			MotionControlModule motion = new HeadingStabilizer(new OmniMotionModule(motors), imu);
			PathFollowingModule follower = new PIDPathFollower();

			// ── Sensing layer ───────────────────────────────────────────────
			var odometry = new FusedOdometryModule(imu, motors);

			// ── Vision / Mapping / Exploration layer ────────────────────────
			var camera    = new RpicamCameraModule(CAMERA_IDX, 640, 480, 30);
			var vision    = new DownwardVisionModule();
			var mapping   = new WallMappingModule();
			var exploration = new FloodFillExplorationModule(Set.of(GOAL));

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				motion.close();
				imu.shutdown();
				pi4j.shutdown();
			}));

			// オドメトリストリーム開始（100 Hz、内部状態を更新）
			odometry.start().subscribe(
					odom -> {},
					err  -> System.err.println("Odometry error: " + err.getMessage())
			);

			// ── 4x4 迷路 探索走行 ───────────────────────────────────────────
			System.out.printf("Starting maze exploration (goal: %s)%n", GOAL);
			var explorer = new MazeExplorer(camera, vision, mapping, exploration, follower, motion, odometry);
			explorer.run();

			System.out.println("Exploration complete.");
			Thread.sleep(500);
			motion.freeRun();
		};
	}
}
