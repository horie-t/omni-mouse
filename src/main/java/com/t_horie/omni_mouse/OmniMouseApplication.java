package com.t_horie.omni_mouse;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.t_horie.omni_mouse.hardware.imu.Bno055IMUModule;
import com.t_horie.omni_mouse.hardware.imu.IMUModule;
import com.t_horie.omni_mouse.sensing.odometry.IMUOdometryModule;
import com.t_horie.omni_mouse.sensing.odometry.OdometryModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class OmniMouseApplication {

	private static final int I2C_BUS = 1;

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			Context pi4j = Pi4J.newAutoContext();
			IMUModule imuModule = new Bno055IMUModule(pi4j, I2C_BUS);
			OdometryModule odometryModule = new IMUOdometryModule(imuModule);

			AtomicInteger counter = new AtomicInteger(0);

			// Start odometry at 100Hz
			var subscription = odometryModule.start()
					.subscribe(odomData -> {
						int count = counter.incrementAndGet();

						// Output at 10Hz (every 10th reading)
						if (count % 10 == 0) {
							System.out.println(odomData);
						}
					});

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				subscription.dispose();
				odometryModule.shutdown();
				imuModule.shutdown();
				pi4j.shutdown();
			}));

			// Keep the application running
			Thread.currentThread().join();
		};
	}
}
