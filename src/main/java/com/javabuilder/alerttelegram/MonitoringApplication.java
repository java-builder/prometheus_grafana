package com.javabuilder.alerttelegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MonitoringApplication {

    static void main(String[] args) {
        SpringApplication.run(MonitoringApplication.class, args);
    }

}
