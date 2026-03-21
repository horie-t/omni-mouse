package com.t_horie.omni_mouse;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;

@SpringBootApplication
public class OmniMouseApplication {

	public static void main(String[] args) {
		SpringApplication.run(OmniMouseApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> Flux.interval(Duration.ofSeconds(1))
				.map(i -> "Hello, world! (" + i + ")")
				.subscribe(IO::println);
	}
}
