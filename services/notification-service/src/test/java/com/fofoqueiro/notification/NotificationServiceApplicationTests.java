// @path services/notification-service/src/test/java/com/fofoqueiro/notification/NotificationServiceApplicationTests.java
// @owner notification-service
// @responsibility Smoke test — stateless, mocks para Kafka/Redis/Mail
package com.fofoqueiro.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class NotificationServiceApplicationTests {

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean JavaMailSender javaMailSender;
    // Kafka consumer beans são mockados para não tentar conectar em bootstrap
    @MockBean KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Test
    void contextLoads() {
    }
}
