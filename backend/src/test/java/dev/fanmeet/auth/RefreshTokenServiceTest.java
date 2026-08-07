package dev.fanmeet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenServiceTest {

    private static final Long USER_ID = 1L;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    void 발급한_토큰은_원문_그대로_저장되지_않는다() {
        String raw = refreshTokenService.issue(USER_ID);

        RefreshToken saved = refreshTokenRepository.findAll().getFirst();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getTokenHash()).isNotEqualTo(raw); // DB가 털려도 그대로 못 쓴다
    }

    @Test
    void 발급한_토큰을_사용하면_주인의_userId를_돌려준다() {
        String raw = refreshTokenService.issue(USER_ID);

        assertThat(refreshTokenService.consume(raw)).isEqualTo(USER_ID);
    }

    @Test
    void 한_번_사용한_토큰은_다시_사용할_수_없다() {
        String raw = refreshTokenService.issue(USER_ID);
        refreshTokenService.consume(raw);

        assertThatThrownBy(() -> refreshTokenService.consume(raw))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void 기기가_달라도_각자의_토큰으로_사용할_수_있다() {
        String pc = refreshTokenService.issue(USER_ID);
        String phone = refreshTokenService.issue(USER_ID);

        refreshTokenService.consume(pc); // PC에서 재발급해도

        assertThatCode(() -> refreshTokenService.consume(phone)) // 폰은 살아 있다
                .doesNotThrowAnyException();
    }
}
