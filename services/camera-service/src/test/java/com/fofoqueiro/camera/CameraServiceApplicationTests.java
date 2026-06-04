// @path services/camera-service/src/test/java/com/fofoqueiro/camera/CameraServiceApplicationTests.java
// @owner camera-service
// @responsibility Smoke test — verifica que o contexto Spring sobe sem erros
package com.fofoqueiro.camera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CameraServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
