package com.tms.controller;

import com.tms.dto.request.ChangePasswordRequest;
import com.tms.dto.request.LoginRequest;
import com.tms.dto.request.RegisterRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.AuthResponse;
import com.tms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, registration, token & OAuth2 APIs")
public class AuthController {

    private final AuthService authService;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-id:}")
    private String microsoftClientId;

    @PostMapping("/login")
    @Operation(summary = "Login with credentials")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(authService.register(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshAccessToken(body.get("refreshToken"))));
    }

    @PatchMapping("/change-password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                               Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @GetMapping("/oauth2/providers")
    @Operation(summary = "Get enabled OAuth2 SSO providers")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getOAuth2Providers() {
        List<Map<String, String>> providers = new ArrayList<>();
        if (isOAuth2Configured(googleClientId)) {
            providers.add(Map.of("name", "google", "label", "Sign in with Google",
                    "url", "/api/v1/auth/oauth2/authorize/google"));
        }
        if (isOAuth2Configured(microsoftClientId)) {
            providers.add(Map.of("name", "microsoft", "label", "Sign in with Microsoft",
                    "url", "/api/v1/auth/oauth2/authorize/microsoft"));
        }
        return ResponseEntity.ok(ApiResponse.ok(providers));
    }

    private boolean isOAuth2Configured(String clientId) {
        return clientId != null && !clientId.isBlank() && !"placeholder".equals(clientId);
    }
}
