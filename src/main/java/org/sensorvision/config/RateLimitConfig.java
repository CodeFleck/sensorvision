package org.sensorvision.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sensorvision.interceptor.RateLimitInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for rate limiting on plugin endpoints.
 * Prevents abuse of plugin installation/uninstallation operations.
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/v1/plugins/**/install")
                .addPathPatterns("/api/v1/plugins/**/uninstall")
                .addPathPatterns("/api/v1/plugins/**/activate")
                .addPathPatterns("/api/v1/plugins/**/deactivate");
    }
}
