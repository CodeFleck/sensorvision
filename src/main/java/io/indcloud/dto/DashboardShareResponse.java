package io.indcloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardShareResponse {
    private Long dashboardId;
    private String dashboardName;
    private Boolean isPublic;
    private String shareUrl;
    private String shareToken;
    private LocalDateTime expiresAt;
    private Boolean isPasswordProtected;
    private Boolean allowAnonymousView;
    private String message;
}
