package org.sensorvision.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for integration tests.
 * Provides mock beans that would otherwise require external dependencies.
 */
@TestConfiguration
public class TestBeanConfiguration {

    /**
     * Provide a mock JavaMailSender for tests.
     * EmailNotificationService uses @Autowired(required = false) but Spring still
     * needs a bean definition for @SpringBootTest integration tests.
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
