// @path services/alert-service/src/main/java/com/fofoqueiro/alert/AlertServiceApplication.java
// @owner alert-service
// @responsibility Entry point do Alert Service — alertas Kafka, WebSocket STOMP hub, Redis pub/sub
// @see docs/ARCHITECTURE.md#alert-service
package com.fofoqueiro.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AlertServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
