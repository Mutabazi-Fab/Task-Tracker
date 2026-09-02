package com.throughline.taskmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling turns on Spring's @Scheduled support — needed for TaskStalenessJob,
// the first background job in this app (everything else only ever runs in response to an
// HTTP request).
@SpringBootApplication
@EnableScheduling
public class ThroughlineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThroughlineApplication.class, args);
	}

}
