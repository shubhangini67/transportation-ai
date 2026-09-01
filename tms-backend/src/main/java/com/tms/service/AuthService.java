package com.tms.service;

import com.tms.dto.request.ChangePasswordRequest;
import com.tms.dto.request.LoginRequest;
import com.tms.dto.request.RegisterRequest;
import com.tms.dto.response.AuthResponse;
import com.tms.entity.RefreshToken;
import com.tms.entity.User;
import com.tms.enums.UserRole;
import com.tms.exception.BadRequestException;
import com.tms.exception.DuplicateResourceException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.UserRepository;
import com.tms.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User '{}' logged in successfully", user.getUsername());
        return buildAuthResponse(token, refreshToken.getToken(), user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CLIENT;
        if (role == UserRole.ADMIN) {
            role = UserRole.CLIENT;
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .build();

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User '{}' registered successfully with role {}", user.getUsername(), user.getRole());
        return buildAuthResponse(token, refreshToken.getToken(), user);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenStr) {
        RefreshToken newRefreshToken = refreshTokenService.verifyAndRotate(refreshTokenStr);
        User user = newRefreshToken.getUser();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        String newAccessToken = tokenProvider.generateToken(authentication);

        return buildAuthResponse(newAccessToken, newRefreshToken.getToken(), user);
    }

    private AuthResponse buildAuthResponse(String token, String refreshToken, User user) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user '{}'", username);
    }
}
