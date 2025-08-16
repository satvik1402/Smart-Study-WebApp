package com.smartstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class for SmartStudy platform
 * 
 * @author SmartStudy Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties
public class SmartStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartStudyApplication.class, args);
        System.out.println("🚀 SmartStudy Application started successfully!");
        System.out.println("📚 Intelligent Learning Platform is running on http://localhost:8080");
    }
}

