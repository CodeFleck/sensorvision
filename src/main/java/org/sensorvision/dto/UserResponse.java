package org.sensorvision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private UUID uuid;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Long organizationId;
    private String organizationName;
    private Set<String> roles;
    private Boolean enabled;
    private String avatarUrl;
    private Long avatarVersion;
    private String themePreference;
}
