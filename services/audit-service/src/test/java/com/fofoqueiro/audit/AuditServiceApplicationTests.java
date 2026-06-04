// @path services/audit-service/src/test/java/com/fofoqueiro/audit/AuditServiceApplicationTests.java
// @owner audit-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
