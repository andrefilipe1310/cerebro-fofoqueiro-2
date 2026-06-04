// @path services/camera-service/src/main/java/com/fofoqueiro/camera/CameraServiceApplication.java
// @owner camera-service
// @responsibility Entry point do Camera Service — CRUD câmeras, stream URL, webhook MediaMTX
// @see docs/ARCHITECTURE.md#camera-service
package com.fofoqueiro.camera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CameraServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CameraServiceApplication.class, args);
    }
}
