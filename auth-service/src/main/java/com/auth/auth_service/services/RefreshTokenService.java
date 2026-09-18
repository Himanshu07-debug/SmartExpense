package com.auth.auth_service.services;

import com.auth.auth_service.entities.RefreshToken;
import com.auth.auth_service.entities.UserInfo;
import com.auth.auth_service.repositories.RefreshTokenRepository;
import com.auth.auth_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    public RefreshToken createRefreshToken(String username) {
        UserInfo userInfo = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        RefreshToken refreshToken = RefreshToken.builder()
                .userInfo(userInfo)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(60 * 60 * 24 * 7)) // 7 Days validity
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException(token.getToken() + " Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}