package com.tms.dto.request;

import com.tms.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleChangeRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}

