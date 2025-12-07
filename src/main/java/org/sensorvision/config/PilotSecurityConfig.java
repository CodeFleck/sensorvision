package org.sensorvision.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.time.Duration;

/**
 * Enhanced security configuration for SensorVision Pilot Program
 * Implements additional security measures required for production pilot deployment
 */
@Configuration
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotSecurityConfig {

    @Value("${security.headers.hsts.max-age:31536000}")
    private long hstsMaxAge;

    @Value("${security.headers.csp.policy:default-src 'self'}")
    private String cspPolicy;

    @Value("${security.session.timeout.minutes:480}")
    private int sessionTimeoutMinutes;

    @Value("${security.remember-me.token-validity-seconds:1209600}")
    private int rememberMeTokenValiditySeconds;

    @Value("${security.remember-me.key:pilot-remember-me-key}")
    private String rememberMeKey;

    /**
     * Enhanced security headers for pilot program
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/actuator/health", "/actuator/prometheus")
                .requestMatchers("/static/**", "/assets/**", "/*.ico", "/*.png", "/*.svg");
    }

    /**
     * Pilot-specific security filter chain with enhanced protections
     */
    @Bean
    public SecurityFilterChain pilotSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Enhanced security headers
                .headers(headers -> headers
                        .frameOptions().deny()
                        .contentTypeOptions().and()
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(hstsMaxAge)
                                .includeSubdomains(true)
                                .preload(true)
                        )
                        .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        .crossOriginEmbedderPolicy("require-corp")
                        .crossOriginOpenerPolicy("same-origin")
                        .crossOriginResourcePolicy("same-origin")
                        .and()
                        .addHeaderWriter(new XXssProtectionHeaderWriter())
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("Content-Security-Policy", cspPolicy);
                            response.setHeader("Permissions-Policy", 
                                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            response.setHeader("X-Frame-Options", "DENY");
                            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                            response.setHeader("Pragma", "no-cache");
                            response.setHeader("Expires", "0");
                        })
                )
                
                // Session management with timeout and concurrent session control
                .sessionManagement(session -> session
                        .maximumSessions(3) // Max 3 concurrent sessions per user
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())
                        .and()
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login?expired=true")
                        .sessionFixation().migrateSession()
                )
                
                // Remember me functionality with secure settings
                .rememberMe(rememberMe -> rememberMe
                        .key(rememberMeKey)
                        .tokenValiditySeconds(rememberMeTokenValiditySeconds)
                        .userDetailsService(customUserDetailsService())
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("PILOT_REMEMBER_ME")
                        .useSecureCookie(true)
                )
                
                // Logout configuration
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/v1/auth/logout", "POST"))
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID", "PILOT_REMEMBER_ME")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                
                // Enhanced authorization rules for pilot
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers("/api/v1/auth/verify-email").permitAll()
                        
                        // Health and monitoring (restricted in pilot)
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        
                        // Pilot-specific endpoints
                        .requestMatchers("/api/v1/pilot/**").hasAnyRole("PILOT_ADMIN", "ADMIN")
                        .requestMatchers("/api/v1/pilot/feedback/**").authenticated()
                        .requestMatchers("/api/v1/pilot/analytics/**").hasAnyRole("PILOT_ADMIN", "ADMIN")
                        
                        // Device ingestion (uses device token auth)
                        .requestMatchers("/api/v1/ingest/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        
                        // WebSocket connections
                        .requestMatchers("/ws/**").authenticated()
                        
                        // Static resources
                        .requestMatchers("/", "/index.html", "/login", "/register").permitAll()
                        .requestMatchers("/pilot/**", "/assets/**", "/static/**").permitAll()
                        .requestMatchers("/*.js", "/*.css", "/*.ico", "/*.png", "/*.svg").permitAll()
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                .build();
    }

    /**
     * Session registry for concurrent session control
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * HTTP session event publisher for session management
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Session authentication strategy for concurrent session control
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry());
    }

    /**
     * Remember me services with enhanced security
     */
    @Bean
    public RememberMeServices rememberMeServices(PasswordEncoder passwordEncoder) {
        TokenBasedRememberMeServices rememberMeServices = 
            new TokenBasedRememberMeServices(rememberMeKey, customUserDetailsService());
        rememberMeServices.setTokenValiditySeconds(rememberMeTokenValiditySeconds);
        rememberMeServices.setParameter("remember-me");
        rememberMeServices.setCookieName("PILOT_REMEMBER_ME");
        rememberMeServices.setUseSecureCookie(true);
        return rememberMeServices;
    }

    // Placeholder for custom user details service - will be injected
    private org.sensorvision.security.CustomUserDetailsService customUserDetailsService() {
        // This will be autowired in the actual implementation
        return null;
    }
}