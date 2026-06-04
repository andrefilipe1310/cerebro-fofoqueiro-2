// @path services/tenant-service/src/test/java/com/fofoqueiro/tenant/TenantServiceApplicationTests.java
// @owner tenant-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TenantServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
