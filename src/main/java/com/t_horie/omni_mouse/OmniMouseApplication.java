package com.t_horie.omni_mouse;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiBus;
import com.t_horie.omni_mouse.control.motion.MotionControlModule;
import com.t_horie.omni_mouse.control.motion.OmniMotionModule;
import com.t_horie.omni_mouse.control.stabilization.HeadingStabilizer;
import com.t_horie.omni_mouse.hardware.imu.Bno055IMUModule;
import com.t_horie.omni_mouse.hardware.motor.L6470MotorModule;
import com.t_horie.omni_mouse.sensing.odometry.FusedOdometryModule;
import com.t_horie.omni_mouse.sensing.odometry.OdometryData;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OmniMouseApplication {

	private static final SpiBus       SPI_BUS = SpiBus.BUS_0;
	private static final SpiChipSelect SPI_CS  = SpiChipSelect.CS_0;
	private static final byte          KVAL    = 0x40;
	private static final int           I2C_BUS = 1;  // /dev/i2c-1

	private static final double TEST_VX_M_PER_S = 0.05;
	private static final long   ODOM_POLL_MS    = 200; // 5 Hz print rate

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			Context pi4j = Pi4J.newAutoContext();

			// Hardware layer
			var motors = new L6470MotorModule(pi4j, SPI_BUS, SPI_CS, KVAL);
			var imu    = new Bno055IMUModule(pi4j, I2C_BUS);

			// Control layer: OmniMotion wrapped by heading stabilizer
			MotionControlModule motion = new HeadingStabilizer(new OmniMotionModule(motors), imu);

			// Sensing layer — fused odometry
			var odometry = new FusedOdometryModule(imu, motors);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				motion.close();
				imu.shutdown();
				pi4j.shutdown();
			}));

			// Start the odometry stream (non-blocking, runs on scheduler thread)
			odometry.start().subscribe(
					odom -> {}, // updated via getCurrentOdometry() below
					err  -> System.err.println("Odometry error: " + err.getMessage())
			);

			System.out.printf("Moving forward %.2f m/s%n", TEST_VX_M_PER_S);
			motion.move(TEST_VX_M_PER_S, 0.0, 0.0);

			while (!Thread.currentThread().isInterrupted()) {
				OdometryData odom = odometry.getCurrentOdometry();
				System.out.println(odom);
				Thread.sleep(ODOM_POLL_MS);
			}
		};
	}
}
