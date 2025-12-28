package io.indcloud.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

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

    /**
     * Provide a mock ClientRegistrationRepository for OAuth2 security configuration.
     * Required by SecurityConfig.filterChain() when OAuth2 login is configured.
     * Uses a dummy registration since tests don't actually perform OAuth2 flows.
     */
    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        // Use placeholder URLs that clearly indicate they are for testing only
        ClientRegistration dummyRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://test.example.com/oauth2/authorize")
                .tokenUri("https://test.example.com/oauth2/token")
                .userInfoUri("https://test.example.com/oauth2/userinfo")
                .userNameAttributeName("sub")
                .clientName("Test OAuth2 Provider")
                .build();

        return new InMemoryClientRegistrationRepository(dummyRegistration);
    }
}
