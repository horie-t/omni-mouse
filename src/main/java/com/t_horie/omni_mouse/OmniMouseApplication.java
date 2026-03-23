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

	// SPI bus 0, CE0 (/dev/spidev0.0)
	private static final SpiBus SPI_BUS = SpiBus.BUS_0;
	private static final SpiChipSelect SPI_CS = SpiChipSelect.CS_0;

	// Motor coil voltage ratio: 0x40 = 25% of Vcc.
	// Adjust if the motor doesn't move (too low) or overheats (too high).
	private static final byte KVAL = 0x40;

	// Target forward speed for verification test (m/s)
	private static final double TEST_VX_M_PER_S = 0.05;

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			Context pi4j = Pi4J.newAutoContext();
			var motors = new L6470MotorModule(pi4j, SPI_BUS, SPI_CS, KVAL);
			MotionControlModule motion = new OmniMotionModule(motors);

			System.out.printf("Moving forward %.2f m/s%n", TEST_VX_M_PER_S);
			motion.move(TEST_VX_M_PER_S, 0.0, 0.0);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				motion.close();
				pi4j.shutdown();
			}));

			// Keep the application running until Ctrl+C
			Thread.currentThread().join();
		};
	}
}
