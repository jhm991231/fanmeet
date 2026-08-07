package dev.fanmeet.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 살아 있는 refresh 토큰 한 건.
 *
 * <p>유저당 여러 행이 공존한다 — PC와 폰에서 각자 로그인 상태를 유지하기 위함이다.
 * 재발급(rotation)은 사용한 그 행만 지우므로 다른 기기의 로그인은 유지된다.
 *
 * <p>토큰 원문은 저장하지 않는다. DB가 유출돼도 그대로 재사용할 수 없도록 해시만 남긴다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User 연관관계 대신 식별자만 든다. 재발급 때 필요한 것은 "누구의 토큰인가" 뿐이고
     * 유저의 다른 정보를 타고 갈 일이 없어, 조회마다 User를 끌어오지 않으려는 선택이다.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 조회 키다. 같은 해시가 두 번 저장될 일은 없으므로 unique. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder
    private RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
