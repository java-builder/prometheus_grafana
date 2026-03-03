package com.javabuilder.alerttelegram.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
@Slf4j(topic = "MONITORING-TEST-CONTROLLER")
public class MonitoringController {

    @GetMapping("/slow")
    ResponseEntity<String> slowResponse(@RequestParam(defaultValue = "2000") int delayMs) throws InterruptedException {
        log.info("Simulating slow response with delay: {}ms", delayMs);
        Thread.sleep(delayMs);
        return ResponseEntity.ok("Response completed after " + delayMs + "ms delay");
    }

    @GetMapping("/error")
    ResponseEntity<String> triggerError() {
        log.error("Triggering 500 error for demo");
        throw new RuntimeException("Simulated 500 error for Prometheus alert testing");
    }

    @GetMapping("/health")
    ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is running");
    }
}
