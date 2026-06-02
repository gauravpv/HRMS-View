package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAutoConfiguration
@SpringBootApplication
@EnableScheduling
public class HrmsViewApplication {

	public static void main(String[] args) {
		SpringApplication.run(HrmsViewApplication.class, args);
	}

}
