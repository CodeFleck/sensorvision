package org.sensorvision.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StreamUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        logger.error("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Invalid argument: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        logger.error("Validation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        logger.error("Constraint violation: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Constraint Violation");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(HibernateException.class)
    public ProblemDetail handleHibernateException(HibernateException ex) {
        logger.error("Database error occurred: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Database Operation Failed");
        problem.setDetail("Unable to complete the database operation. Please try again or contact support if the issue persists.");
        problem.setProperty("developerMessage", ex.getMessage());
        problem.setProperty("errorType", ex.getClass().getSimpleName());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Data Conflict");
        problem.setDetail("The operation conflicts with existing data. This might be due to duplicate entries or related data constraints.");
        problem.setProperty("developerMessage", ex.getMostSpecificCause().getMessage());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Access Denied");
        problem.setDetail("You don't have permission to access this resource.");
        problem.setProperty("developerMessage", ex.getMessage());
        return problem;
    }

    /**
     * Handle authentication exceptions - user account disabled
     */
    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        logger.warn("Login attempt with disabled account");
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Account Disabled");
        problem.setDetail("Your account has been disabled. Please contact support for assistance.");
        return problem;
    }

    /**
     * Handle authentication exceptions - user not found
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFound(UsernameNotFoundException ex) {
        logger.warn("Login attempt with non-existent username");
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Invalid Credentials");
        problem.setDetail("Invalid username or password. Please check your credentials and try again.");
        return problem;
    }

    /**
     * Handle authentication exceptions - bad credentials (wrong password or username)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Login attempt with invalid credentials");
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Invalid Credentials");
        problem.setDetail("Invalid username or password. Please check your credentials and try again.");
        return problem;
    }

    /**
     * Handle 404 errors for SPA routes - serve index.html for non-API routes
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        // For non-API routes, serve the React SPA (index.html)
        if (!requestUri.startsWith("/api/") && !requestUri.startsWith("/actuator/")) {
            logger.debug("Serving SPA for route: {}", requestUri);
            Resource resource = new ClassPathResource("/static/index.html");

            try {
                if (resource.exists()) {
                    byte[] content = StreamUtils.copyToByteArray(resource.getInputStream());
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(content);
                }
            } catch (IOException ioEx) {
                logger.error("Failed to read index.html", ioEx);
            }
        }

        // For API routes or if index.html not found, return 404 error as JSON
        logger.error("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource Not Found");
        problem.setDetail("The requested resource was not found.");
        problem.setProperty("developerMessage", ex.getMessage());
        problem.setProperty("errorType", "NoResourceFoundException");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Something Went Wrong");
        problem.setDetail("An unexpected error occurred while processing your request. Our team has been notified. Please try again later.");
        problem.setProperty("developerMessage", ex.getMessage());
        problem.setProperty("errorType", ex.getClass().getSimpleName());
        return problem;
    }
}
