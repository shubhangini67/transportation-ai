package com.tms.controller;

import com.tms.dto.request.RoleChangeRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.UserResponse;
import com.tms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs (Admin only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(page, size)));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleChangeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.changeRole(id, request.getRole(), authentication.getName())));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.deactivateUser(id, authentication.getName())));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a user")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.activateUser(id)));
    }
}

