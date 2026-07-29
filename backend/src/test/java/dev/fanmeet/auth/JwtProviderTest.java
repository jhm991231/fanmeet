package dev.fanmeet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.fanmeet.user.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    // HS256 서명은 최소 256비트(32바이트) 시크릿을 요구한다
    private static final String SECRET = "fanmeet-test-secret-key-must-be-32-bytes!";

    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(SECRET, 1800, 1209600));

    @Test
    void 발급한_액세스_토큰에서_userId와_role을_복원한다() {
        String token = jwtProvider.createAccessToken(17L, Role.HOST);

        assertThat(jwtProvider.parseUserId(token)).isEqualTo(17L);
        assertThat(jwtProvider.parseRole(token)).isEqualTo(Role.HOST);
    }

    @Test
    void 만료된_토큰은_예외가_난다() {
        JwtProvider expiredIssuer = new JwtProvider(new JwtProperties(SECRET, -1, -1)); // 유효기간 음수 = 발급 즉시 만료

        String token = expiredIssuer.createAccessToken(17L, Role.FAN);

        assertThatThrownBy(() -> jwtProvider.parseUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 다른_시크릿으로_서명한_토큰은_검증에_실패한다() {
        JwtProvider attacker = new JwtProvider(new JwtProperties("attacker-secret-key-also-32-bytes-long!!", 1800, 1209600));

        String token = attacker.createAccessToken(17L, Role.FAN);

        assertThatThrownBy(() -> jwtProvider.parseUserId(token))
                .isInstanceOf(SignatureException.class);
    }
}
