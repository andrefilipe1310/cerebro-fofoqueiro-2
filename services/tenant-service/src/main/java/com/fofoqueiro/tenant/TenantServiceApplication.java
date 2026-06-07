// @path services/tenant-service/src/main/java/com/fofoqueiro/tenant/TenantServiceApplication.java
// @owner tenant-service
// @responsibility Entry point do Tenant Service — CRUD tenants/usuários, white-label, limites de plano
// @see docs/ARCHITECTURE.md#tenant-service
package com.fofoqueiro.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TenantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}
