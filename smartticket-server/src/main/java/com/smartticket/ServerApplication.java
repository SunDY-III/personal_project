package com.smartticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.smartticket")
@EnableAsync
public class ServerApplication {
    public static void main(String[] args) { SpringApplication.run(ServerApplication.class, args); }
}
