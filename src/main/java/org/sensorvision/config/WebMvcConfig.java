package org.sensorvision.config;

import org.sensorvision.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",  // Exclude auth endpoints
                        "/api/v1/actuator/**",  // Exclude actuator endpoints
                        "/api/v1/devices/**",  // Exclude device endpoints (high frequency reads)
                        "/api/v1/data/**",  // Exclude telemetry data endpoints (high frequency reads)
                        "/api/v1/ingest/**",  // Exclude data ingestion endpoints
                        "/swagger-ui/**",  // Exclude Swagger UI
                        "/v3/api-docs/**"  // Exclude OpenAPI docs
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve React SPA - forward all non-API routes to index.html for client-side routing
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // If the resource exists (like CSS, JS, images), serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // For all other paths (React Router routes), serve index.html
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
