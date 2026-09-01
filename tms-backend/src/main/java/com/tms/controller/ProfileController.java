package com.tms.controller;

import com.tms.dto.request.ProfileUpdateRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.UserResponse;
import com.tms.entity.User;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.UserRepository;
import com.tms.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile APIs")
public class ProfileController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", auth.getName()));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(user)));
    }

    @PutMapping
    @Operation(summary = "Update profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication auth, @Valid @RequestBody ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", auth.getName()));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(user)));
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload avatar image")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            Authentication auth, @RequestParam("file") MultipartFile file) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", auth.getName()));
        String avatarUrl = fileStorageService.storeFile(file);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(user)));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId()).username(u.getUsername()).email(u.getEmail())
                .fullName(u.getFullName()).role(u.getRole()).active(u.getActive())
                .avatarUrl(u.getAvatarUrl())
                .createdAt(u.getCreatedAt()).build();
    }
}

