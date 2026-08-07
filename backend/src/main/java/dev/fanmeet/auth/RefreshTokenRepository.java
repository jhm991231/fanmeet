package dev.fanmeet.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 로그아웃 시 그 유저의 토큰을 모두 폐기할 때 쓴다. */
    void deleteByUserId(Long userId);
}
