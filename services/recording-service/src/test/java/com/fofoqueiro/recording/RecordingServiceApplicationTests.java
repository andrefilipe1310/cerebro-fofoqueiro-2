// @path services/recording-service/src/test/java/com/fofoqueiro/recording/RecordingServiceApplicationTests.java
// @owner recording-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.recording;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RecordingServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
