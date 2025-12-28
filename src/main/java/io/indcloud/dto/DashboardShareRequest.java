package io.indcloud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardShareRequest {
    @NotNull(message = "isPublic flag is required")
    private Boolean isPublic;

    private String password; // Optional password protection
    private LocalDateTime expiresAt; // Optional expiration date
    private Boolean allowAnonymousView; // Whether unauthenticated users can view
}
