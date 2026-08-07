package dev.fanmeet.auth;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * refresh 토큰을 담는 쿠키를 만든다.
 *
 * <p>로그인 성공(OAuth2SuccessHandler), 재발급, 로그아웃 세 곳에서 같은 쿠키를 다루므로
 * 설정을 여기 한 곳에 모은다. 흩어져 있으면 SameSite 같은 속성을 바꿀 때 한쪽만 고치는
 * 사고가 난다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";

    /** 재발급·로그아웃 API 에만 실려 가도록 범위를 제한한다. /api/me 요청에는 붙지 않는다. */
    private static final String PATH = "/api/auth";

    private final JwtProperties jwtProperties;

    public ResponseCookie of(String rawToken) {
        return base(rawToken)
                .maxAge(Duration.ofSeconds(jwtProperties.refreshValiditySeconds()))
                .build();
    }

    /**
     * 로그아웃용. 같은 이름·경로로 수명 0 인 쿠키를 덮어씌워 브라우저가 즉시 지우게 한다.
     * 이름이나 경로가 다르면 원래 쿠키가 그대로 남는다.
     */
    public ResponseCookie expired() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                // JS 가 읽을 수 없다. XSS 로 스크립트가 심겨도 토큰은 못 가져간다.
                .httpOnly(true)
                // 로컬은 http 라 false. 배포 환경에선 true 로 전환해야 한다.
                .secure(false)
                .path(PATH)
                // 다른 사이트에서 시작된 POST 에는 실리지 않는다. CSRF 방어.
                // 배포 시 프론트와 백엔드의 등록 도메인이 갈리면 이 값으로는 쿠키가
                // 아예 안 실린다(SameSite 는 포트가 아니라 eTLD+1 로 판정한다).
                .sameSite("Lax");
    }
}
