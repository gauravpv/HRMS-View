package com.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableAutoConfiguration
@SpringBootApplication
public class HrmsViewApplication {

	private static final Logger logger = LogManager.getLogger(HrmsViewApplication.class);

	public static void main(String[] args) {
		logger.info("Starting HRMS View application");
		SpringApplication.run(HrmsViewApplication.class, args);
		logger.info("HRMS View application started");
	}

}
