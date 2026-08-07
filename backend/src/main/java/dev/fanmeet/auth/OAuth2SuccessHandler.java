package dev.fanmeet.auth;

import dev.fanmeet.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookie refreshTokenCookie;
    private final String frontUrl;

    public OAuth2SuccessHandler(JwtProvider jwtProvider,
                                RefreshTokenService refreshTokenService,
                                RefreshTokenCookie refreshTokenCookie,
                                @Value("${app.front-url}") String frontUrl) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookie = refreshTokenCookie;
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
        // issue()가 DB 저장까지 한다. 저장하지 않으면 재발급 때 조회에서 걸려 항상 실패한다.
        String refreshToken = refreshTokenService.issue(userId);

        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookie.of(refreshToken).toString());

        // 프래그먼트(#)는 서버 로그에 남지 않고 브라우저 안에서만 다뤄진다
        response.sendRedirect(frontUrl + "/oauth/callback#accessToken=" + accessToken);
    }
}
