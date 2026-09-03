package com.cartvia.cartvia_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CartviaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartviaBackendApplication.class, args);
	}

}
