package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.sensorvision.model.IssueCategory;
import org.sensorvision.model.IssueSeverity;

/**
 * DTO for submitting a new issue/bug report
 */
public record IssueSubmissionRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotNull(message = "Category is required")
    IssueCategory category,

    @NotNull(message = "Severity is required")
    IssueSeverity severity,

    // Optional screenshot (base64 encoded)
    String screenshotBase64,

    String screenshotFilename,

    // Browser and context information
    String browserInfo,

    String pageUrl,

    String userAgent,

    String screenResolution
) {
}
