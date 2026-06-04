// @path services/recording-service/src/main/java/com/fofoqueiro/recording/RecordingServiceApplication.java
// @owner recording-service
// @responsibility Entry point do Recording Service — metadados de gravações, timeline, signed URLs R2
// @see docs/ARCHITECTURE.md#recording-service
package com.fofoqueiro.recording;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecordingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecordingServiceApplication.class, args);
    }
}
