// @path services/chms-service/src/main/java/com/fofoqueiro/chms/ChmsServiceApplication.java
// @owner chms-service
// @responsibility Entry point do CHMS Service — Camera Health Monitoring, polling MediaMTX 30s
// @see docs/ARCHITECTURE.md#chms-service
package com.fofoqueiro.chms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChmsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChmsServiceApplication.class, args);
    }
}
