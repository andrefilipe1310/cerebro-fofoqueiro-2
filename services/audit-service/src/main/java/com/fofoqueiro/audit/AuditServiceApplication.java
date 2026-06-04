// @path services/audit-service/src/main/java/com/fofoqueiro/audit/AuditServiceApplication.java
// @owner audit-service
// @responsibility Entry point do Audit Service — append-only logs imutáveis, consumer Kafka
// @see docs/ARCHITECTURE.md#audit-service | docs/SDD.md#design-audit
package com.fofoqueiro.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
