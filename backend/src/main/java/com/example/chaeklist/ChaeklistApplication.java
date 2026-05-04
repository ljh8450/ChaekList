package com.example.chaeklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ChaeklistApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChaeklistApplication.class, args);
	}

}
