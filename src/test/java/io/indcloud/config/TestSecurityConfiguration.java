package io.indcloud.config;

import io.indcloud.security.UserPrincipal;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

/**
 * Test security configuration for unit tests.
 * Provides a mock UserDetailsService that returns a test user for authentication.
 */
@TestConfiguration
public class TestSecurityConfiguration {

    /**
     * Creates a mock UserDetailsService that returns a UserPrincipal for authentication.
     * This allows @WithUserDetails to work properly in controller tests.
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        return username -> {
            // Return UserPrincipal for Spring Security authentication
            // The test will mock SecurityUtils and UserRepository to return the actual User entity
            return new UserPrincipal(
                    1L,  // ID
                    username,  // Username
                    "test@example.com",  // Email
                    "$2a$10$test",  // Password hash
                    1L,  // Organization ID
                    true,  // Enabled
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        };
    }
}
