package dev.fanmeet.auth;

import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * refresh 토큰으로 access 토큰을 재발급하고, 로그아웃을 처리한다.
 *
 * <p>이 두 엔드포인트는 access 토큰 없이 호출된다(만료돼서 부르는 것이므로). 인증은
 * {@code JwtAuthenticationFilter}가 아니라 여기서 쿠키의 refresh 토큰으로 직접 한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookie refreshTokenCookie;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * 재발급. 쓴 토큰은 폐기하고 새 토큰을 함께 내려보낸다(rotation).
     *
     * <p>구 토큰을 지우기 때문에, 같은 토큰이 다시 오면 조회에서 걸린다. 정상 흐름에서는
     * 있을 수 없는 일이므로 탈취 신호로 볼 수 있다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String rawToken) {

        if (rawToken == null) {
            throw new InvalidRefreshTokenException("refresh 토큰 쿠키가 없습니다");
        }

        Long userId = refreshTokenService.consume(rawToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRefreshTokenException("토큰의 주인을 찾을 수 없습니다"));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String newRefreshToken = refreshTokenService.issue(user.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.of(newRefreshToken).toString())
                .body(new TokenResponse(accessToken));
    }

    /**
     * 로그아웃. 이 기기의 토큰만 폐기하고 쿠키를 지운다.
     *
     * <p>쿠키가 없거나 이미 폐기된 토큰이어도 200이다. 로그아웃은 멱등해야 한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String rawToken) {

        if (rawToken != null) {
            refreshTokenService.revoke(rawToken);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.expired().toString())
                .build();
    }
}
