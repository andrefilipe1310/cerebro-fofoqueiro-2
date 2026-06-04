// @path services/auth-service/src/main/java/com/fofoqueiro/auth/AuthServiceApplication.java
// @owner auth-service
// @responsibility Entry point do Auth Service — login, JWT, 2FA TOTP, refresh tokens
// @see docs/ARCHITECTURE.md#auth-service
package com.fofoqueiro.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
