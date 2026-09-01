package com.tms.service;

import com.tms.dto.response.AuthResponse;
import com.tms.entity.RefreshToken;
import com.tms.entity.User;
import com.tms.enums.UserRole;
import com.tms.repository.UserRepository;
import com.tms.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse processOAuth2Login(String provider, OAuth2User oAuth2User) {
        String providerId = oAuth2User.getAttribute("sub"); // works for both Google and Microsoft OIDC
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerUpper = provider.toUpperCase();

        if (email == null) {
            email = providerId + "@" + provider + ".oauth";
        }
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        // 1. Try to find by OAuth provider + id
        Optional<User> existingOAuth = userRepository.findByOauthProviderAndOauthProviderId(providerUpper, providerId);
        if (existingOAuth.isPresent()) {
            return issueTokens(existingOAuth.get());
        }

        // 2. Try to find by email (auto-link existing account)
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            user.setOauthProvider(providerUpper);
            user.setOauthProviderId(providerId);
            userRepository.save(user);
            log.info("Linked OAuth provider {} to existing user '{}'", providerUpper, user.getUsername());
            return issueTokens(user);
        }

        // 3. Create new user
        String username = generateUniqueUsername(email);
        User newUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // random password
                .fullName(name)
                .role(UserRole.CLIENT) // default role for OAuth sign-ups
                .oauthProvider(providerUpper)
                .oauthProviderId(providerId)
                .build();
        userRepository.save(newUser);
        log.info("Created new OAuth user '{}' via {} provider", username, providerUpper);
        return issueTokens(newUser);
    }

    private AuthResponse issueTokens(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        String jwt = tokenProvider.generateToken(auth);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (base.length() < 3) base = base + "user";
        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + counter++;
        }
        return candidate;
    }
}

