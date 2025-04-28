package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = SpringApplicationAdminJmxAutoConfiguration.class)
@EnableScheduling
public class HomeFlex1Application {

	public static void main(String[] args) {
		SpringApplication.run(HomeFlex1Application.class, args);
	}

}
