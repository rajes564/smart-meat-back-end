package com.smartmeat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartMeatApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartMeatApplication.class, args);
    }
}
