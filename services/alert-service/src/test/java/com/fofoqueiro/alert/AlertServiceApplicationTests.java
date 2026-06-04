// @path services/alert-service/src/test/java/com/fofoqueiro/alert/AlertServiceApplicationTests.java
// @owner alert-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.alert;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AlertServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
