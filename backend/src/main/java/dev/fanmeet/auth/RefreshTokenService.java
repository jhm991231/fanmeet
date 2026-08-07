package dev.fanmeet.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * refresh 토큰의 발급과 소비를 맡는다.
 *
 * <p>access 토큰과 달리 refresh 토큰은 <b>JWT가 아니다.</b> 어차피 매번 DB를 조회해
 * 살아 있는 토큰인지 확인하므로, 토큰이 자기 정보를 품고 다닐 이유가 없다. 무작위
 * 문자열이면 충분하고, 오히려 유출돼도 userId 같은 정보가 새지 않는다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 새 refresh 토큰을 발급해 해시로 저장하고, 원문을 돌려준다. */
    @Transactional
    public String issue(Long userId) {
        String rawToken = generateToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plusSeconds(jwtProperties.refreshValiditySeconds()))
                .build());

        return rawToken;
    }

    /**
     * 토큰을 한 번 소비한다(단발성). 유효하면 주인의 userId를 돌려주고 그 토큰은 폐기한다.
     *
     * <p>쓰는 즉시 지우기 때문에, 같은 토큰이 다시 오면 조회에서 걸린다. 정상 흐름에서는
     * 있을 수 없는 일이므로 탈취 신호로 볼 수 있다.
     *
     * @throws InvalidRefreshTokenException 없거나 만료됐거나 이미 사용된 토큰
     */
    @Transactional
    public Long consume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "존재하지 않거나 이미 사용된 refresh 토큰입니다"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            // 만료된 행은 여기서 지우지 않는다. 예외로 롤백되기 때문이며,
            // 어차피 정리 스케줄러가 걷어간다.
            throw new InvalidRefreshTokenException("만료된 refresh 토큰입니다");
        }

        refreshTokenRepository.delete(token);
        return token.getUserId();
    }

    /**
     * 토큰을 조용히 폐기한다. 로그아웃용이다.
     *
     * <p>{@link #consume}과 달리 없는 토큰이어도 예외를 던지지 않는다. 로그아웃은 멱등해야
     * 한다 — 버튼을 두 번 눌렀거나 이미 만료된 토큰으로 로그아웃한다고 실패시킬 이유가 없다.
     *
     * <p>그 토큰 하나만 지운다. 유저의 토큰을 전부 지우면 PC에서 로그아웃했는데 폰까지 풀린다.
     */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    /** 32바이트 무작위. Base64 URL 인코딩이라 쿠키에 그대로 실을 수 있다. */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256으로 해시한다. 비밀번호에 쓰는 BCrypt를 쓰지 않는 이유는 두 가지다.
     *
     * <p>첫째, BCrypt는 <b>매번 다른 소금(salt)을 섞으므로</b> 같은 토큰이라도 해시가
     * 달라진다. 그러면 해시로 행을 찾을 수 없어 전체를 훑으며 대조해야 한다.
     *
     * <p>둘째, BCrypt의 느림은 사람이 만든 짧은 비밀번호를 무차별 대입에서 지키려는
     * 설계다. 이 토큰은 32바이트 무작위라 애초에 대입이 불가능하므로 느릴 이유가 없다.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 자바 구현이 반드시 제공한다. 여기 올 일은 없다.
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다", e);
        }
    }
}
