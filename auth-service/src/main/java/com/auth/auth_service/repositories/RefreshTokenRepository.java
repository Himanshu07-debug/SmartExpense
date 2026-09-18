package com.auth.auth_service.repositories;

import com.auth.auth_service.entities.RefreshToken;
import com.auth.auth_service.entities.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    int deleteByUserInfo(UserInfo userInfo);
}
