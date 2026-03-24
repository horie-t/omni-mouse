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
import com.t_horie.omni_mouse.hardware.imu.Bno055IMUModule;
import com.t_horie.omni_mouse.hardware.motor.L6470MotorModule;
import com.t_horie.omni_mouse.sensing.odometry.FusedOdometryModule;
import com.t_horie.omni_mouse.sensing.odometry.Pose;
import com.t_horie.omni_mouse.sensing.odometry.Velocity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class OmniMouseApplication {

	private static final SpiBus        SPI_BUS = SpiBus.BUS_0;
	private static final SpiChipSelect SPI_CS  = SpiChipSelect.CS_0;
	private static final byte          KVAL    = 0x40;
	private static final int           I2C_BUS = 1;

	// Control loop rate
	private static final long CONTROL_PERIOD_MS = 10; // 100 Hz

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
			// OmniMotion wrapped by heading stabilizer (transparent to callers)
			MotionControlModule motion = new HeadingStabilizer(new OmniMotionModule(motors), imu);

			// Path follower (pure PD, no IMU dependency)
			PathFollowingModule follower = new PIDPathFollower();

			// ── Sensing layer ───────────────────────────────────────────────
			var odometry = new FusedOdometryModule(imu, motors);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				motion.close();
				imu.shutdown();
				pi4j.shutdown();
			}));

			// Start odometry stream at 100 Hz (updates currentOdometry internally)
			odometry.start().subscribe(
					odom -> {},
					err  -> System.err.println("Odometry error: " + err.getMessage())
			);

			// ── Test: navigate a square (0.3 m side, back to origin) ────────
			List<Pose> square = List.of(
					new Pose(0.30,  0.00, 0.00),          // forward 30 cm
					new Pose(0.30,  0.30, Math.PI / 2),   // left 30 cm, face left
					new Pose(0.00,  0.30, Math.PI),       // back 30 cm, face backward
					new Pose(0.00,  0.00, 0.00)           // return to origin, face forward
			);

			System.out.println("Starting square path test (0.3 m side)");
			follower.setPath(square);

			// 100 Hz closed-loop control
			while (!follower.isComplete() && !Thread.currentThread().isInterrupted()) {
				Pose     pose = odometry.getCurrentOdometry().pose();
				Velocity cmd  = follower.computeVelocity(pose);
				motion.move(cmd.vx(), cmd.vy(), cmd.omega());
				System.out.printf("  %s  →  %s%n", pose, cmd);
				Thread.sleep(CONTROL_PERIOD_MS);
			}

			System.out.println("Path complete — stopping");
			motion.stop();
			Thread.sleep(500);
			motion.freeRun();
		};
	}
}
