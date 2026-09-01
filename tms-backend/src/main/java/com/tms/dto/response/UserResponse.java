package com.tms.dto.response;

import com.tms.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
    private Boolean active;
    private String avatarUrl;
    private LocalDateTime createdAt;
}

