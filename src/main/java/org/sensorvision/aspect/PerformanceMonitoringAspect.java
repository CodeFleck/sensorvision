package org.sensorvision.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sensorvision.service.PilotPerformanceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Performance monitoring aspect for pilot program optimization.
 * Automatically tracks execution time and performance metrics for key operations.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pilot.performance.metrics-enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitoringAspect {

    private final PilotPerformanceService performanceService;

    /**
     * Monitor telemetry service operations
     */
    @Around("execution(* org.sensorvision.service.TelemetryService.*(..))")
    public Object monitorTelemetryService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "telemetry");
    }

    /**
     * Monitor device service operations
     */
    @Around("execution(* org.sensorvision.service.DeviceService.*(..))")
    public Object monitorDeviceService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "device");
    }

    /**
     * Monitor analytics service operations
     */
    @Around("execution(* org.sensorvision.service.AnalyticsService.*(..))")
    public Object monitorAnalyticsService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "analytics");
    }

    /**
     * Monitor pilot-specific service operations
     */
    @Around("execution(* org.sensorvision.service.Pilot*Service.*(..))")
    public Object monitorPilotServices(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "pilot");
    }

    /**
     * Monitor database repository operations
     */
    @Around("execution(* org.sensorvision.repository.*Repository.*(..))")
    public Object monitorRepositoryOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "database");
    }

    /**
     * Monitor REST controller operations
     */
    @Around("execution(* org.sensorvision.controller.*Controller.*(..))")
    public Object monitorControllerOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "api");
    }

    /**
     * Generic method execution monitoring
     */
    private Object monitorExecution(ProceedingJoinPoint joinPoint, String category) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record successful execution
            performanceService.recordMethodExecution(category, methodName, executionTime, true);
            
            // Log slow operations
            if (executionTime > getSlowQueryThreshold()) {
                log.warn("Slow {} operation detected: {} took {}ms", category, methodName, executionTime);
            }
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failed execution
            performanceService.recordMethodExecution(category, methodName, executionTime, false);
            
            log.error("Error in {} operation: {} failed after {}ms", category, methodName, executionTime, throwable);
            throw throwable;
        }
    }

    /**
     * Get slow query threshold from configuration
     */
    private long getSlowQueryThreshold() {
        // Default to 1 second, can be configured via properties
        return 1000L;
    }
}