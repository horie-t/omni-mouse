package com.t_horie.omni_mouse;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;

@SpringBootApplication
public class OmniMouseApplication {

	private static final int LED_PIN = 25;

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			Context pi4j = Pi4J.newAutoContext();

			DigitalOutput led = pi4j.digitalOutput().create(LED_PIN);

			var subscription = Flux.interval(Duration.ofSeconds(1))
					.subscribe(_ -> {
						led.toggle();
						System.out.println("LED " + (led.state() == DigitalState.HIGH ? "ON" : "OFF"));
					});

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down...");
				subscription.dispose();
				pi4j.shutdown();
			}));

			// Keep the application running
			Thread.currentThread().join();
		};
	}
}
