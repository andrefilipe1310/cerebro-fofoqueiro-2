// @path services/notification-service/src/main/java/com/fofoqueiro/notification/NotificationServiceApplication.java
// @owner notification-service
// @responsibility Entry point do Notification Service — e-mail/SMS via Kafka events, throttling Redis
// @see docs/ARCHITECTURE.md#notification-service
package com.fofoqueiro.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
