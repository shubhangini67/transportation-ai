package com.tms.service;

import com.tms.entity.RefreshToken;
import com.tms.entity.User;
import com.tms.exception.BadRequestException;
import com.tms.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")  // 7 days default
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken verifyAndRotate(String tokenStr) {
        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(tokenStr)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (existing.getExpiryDate().isBefore(Instant.now())) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new BadRequestException("Refresh token expired");
        }

        // Revoke the old token
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Create a new one (rotation)
        return createRefreshToken(existing.getUser());
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}

