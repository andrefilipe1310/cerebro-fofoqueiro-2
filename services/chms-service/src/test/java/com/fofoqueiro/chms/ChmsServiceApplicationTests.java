// @path services/chms-service/src/test/java/com/fofoqueiro/chms/ChmsServiceApplicationTests.java
// @owner chms-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.chms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ChmsServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
