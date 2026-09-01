package com.tms.security;

import com.tms.dto.response.AuthResponse;
import com.tms.service.OAuth2AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthenticationService oAuth2AuthService;

    @Value("${app.oauth2.frontend-redirect-url:http://localhost:3000/oauth2/callback}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        try {
            AuthResponse authResponse = oAuth2AuthService.processOAuth2Login(provider, oAuth2User);

            String redirectUrl = frontendRedirectUrl
                    + "?token=" + URLEncoder.encode(authResponse.getToken(), StandardCharsets.UTF_8)
                    + "&refreshToken=" + URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8)
                    + "&username=" + URLEncoder.encode(authResponse.getUsername(), StandardCharsets.UTF_8)
                    + "&email=" + URLEncoder.encode(authResponse.getEmail(), StandardCharsets.UTF_8)
                    + "&fullName=" + URLEncoder.encode(authResponse.getFullName() != null ? authResponse.getFullName() : "", StandardCharsets.UTF_8)
                    + "&role=" + URLEncoder.encode(authResponse.getRole().name(), StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("OAuth2 login failed for provider {}: {}", provider, e.getMessage());
            response.sendRedirect(frontendRedirectUrl + "?error=" + URLEncoder.encode("OAuth login failed", StandardCharsets.UTF_8));
        }
    }
}

