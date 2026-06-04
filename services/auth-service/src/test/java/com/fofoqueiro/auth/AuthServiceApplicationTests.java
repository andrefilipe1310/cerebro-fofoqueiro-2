// @path services/auth-service/src/test/java/com/fofoqueiro/auth/AuthServiceApplicationTests.java
// @owner auth-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
