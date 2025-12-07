package org.sensorvision.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when pilot program quotas or limits are exceeded
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class QuotaExceededException extends RuntimeException {

    private final String quotaType;
    private final Long currentValue;
    private final Long limitValue;

    public QuotaExceededException(String message) {
        super(message);
        this.quotaType = null;
        this.currentValue = null;
        this.limitValue = null;
    }

    public QuotaExceededException(String message, String quotaType, Long currentValue, Long limitValue) {
        super(message);
        this.quotaType = quotaType;
        this.currentValue = currentValue;
        this.limitValue = limitValue;
    }

    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
        this.quotaType = null;
        this.currentValue = null;
        this.limitValue = null;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public Long getCurrentValue() {
        return currentValue;
    }

    public Long getLimitValue() {
        return limitValue;
    }
}