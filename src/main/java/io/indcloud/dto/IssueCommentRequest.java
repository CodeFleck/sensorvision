package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueCommentRequest(
    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    String message,

    boolean internal  // Only admins can set this to true
) {
}
