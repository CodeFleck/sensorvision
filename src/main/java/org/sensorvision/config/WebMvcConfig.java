package org.sensorvision.config;

import lombok.RequiredArgsConstructor;
import org.sensorvision.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",  // Exclude auth endpoints
                        "/api/v1/actuator/**",  // Exclude actuator endpoints
                        "/swagger-ui/**",  // Exclude Swagger UI
                        "/v3/api-docs/**"  // Exclude OpenAPI docs
                );
    }
}
