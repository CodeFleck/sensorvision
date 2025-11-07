package org.sensorvision.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for phone number data
 */
public record PhoneNumberResponse(
    UUID id,
    String phoneNumber,  // Masked for security (e.g., +1555***4567)
    String countryCode,
    Boolean verified,
    Boolean isPrimary,
    Boolean enabled,
    Instant createdAt
) {}
