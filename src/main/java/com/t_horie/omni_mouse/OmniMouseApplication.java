package com.t_horie.omni_mouse;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiBus;
import com.t_horie.omni_mouse.control.motion.MotionControlModule;
import com.t_horie.omni_mouse.control.motion.OmniMotionModule;
import com.t_horie.omni_mouse.hardware.motor.L6470MotorModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OmniMouseApplication {

	private static final SpiBus SPI_BUS = SpiBus.BUS_0;
	private static final SpiChipSelect SPI_CS = SpiChipSelect.CS_0;
	private static final byte KVAL = 0x40;

	private static final double TEST_VX_M_PER_S = 0.05;
	private static final long  ENCODER_POLL_MS   = 500;

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			Context pi4j = Pi4J.newAutoContext();
			var motors = new L6470MotorModule(pi4j, SPI_BUS, SPI_CS, KVAL);
			MotionControlModule motion = new OmniMotionModule(motors);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				motion.close();
				pi4j.shutdown();
			}));

			System.out.printf("Moving forward %.2f m/s — printing encoder every %d ms%n",
					TEST_VX_M_PER_S, ENCODER_POLL_MS);
			motion.move(TEST_VX_M_PER_S, 0.0, 0.0);

			while (!Thread.currentThread().isInterrupted()) {
				double[] pos = motors.getAbsolutePositions();
				System.out.printf("ABS_POS (rev): [0]=%+.3f  [1]=%+.3f  [2]=%+.3f%n",
						pos[0], pos[1], pos[2]);
				Thread.sleep(ENCODER_POLL_MS);
			}
		};
	}
}
