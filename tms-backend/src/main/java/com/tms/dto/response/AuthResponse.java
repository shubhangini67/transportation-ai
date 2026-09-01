package com.tms.dto.response;

import com.tms.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private String refreshToken;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
}
