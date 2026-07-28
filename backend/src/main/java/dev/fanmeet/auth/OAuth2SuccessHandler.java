package dev.fanmeet.auth;

import dev.fanmeet.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 로그인의 마지막 단계: 우리 서비스의 토큰을 발급해 프론트로 돌려보낸다.
 * - access 토큰: URL 프래그먼트로 전달 (프론트가 메모리에 보관)
 * - refresh 토큰: HttpOnly 쿠키로 전달 (JS가 읽을 수 없음)
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final String frontUrl;

    public OAuth2SuccessHandler(JwtProvider jwtProvider, @Value("${app.front-url}") String frontUrl) {
        this.jwtProvider = jwtProvider;
        this.frontUrl = frontUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Long userId = principal.getAttribute("userId");
        Role role = Role.valueOf(principal.getAttribute("role"));

        String accessToken = jwtProvider.createAccessToken(userId, role);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // 로컬은 http라 false, 배포 환경에선 true로 전환 필요
                .path("/api/auth") // 재발급/로그아웃 API에만 실려 가도록 범위 제한
                .maxAge(Duration.ofDays(14))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 프래그먼트(#)는 서버 로그에 남지 않고 브라우저 안에서만 다뤄진다
        response.sendRedirect(frontUrl + "/oauth/callback#accessToken=" + accessToken);
    }
}
