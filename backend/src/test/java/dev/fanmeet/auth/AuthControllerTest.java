package dev.fanmeet.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fanmeet.user.Role;
import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc: 실제 서버(8080)를 띄우지 않고 HTTP 요청/응답을 흉내 내는 도구.
 * 필터 체인과 컨트롤러는 진짜로 실행되므로, 인증 동작을 검증하기에 적합하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    // application-test.yml의 jwt.secret과 같아야 "만료" 상황을 정확히 재현할 수 있다
    @Value("${jwt.secret}")
    String SECRET;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private User user;

    @BeforeEach
    void setUp() {
        // @SpringBootTest 는 롤백해 주지 않으므로 테스트마다 직접 비운다
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .provider("kakao").providerId("12345").nickname("현민").role(Role.FAN)
                .build());
    }

    @Test
    void 토큰_없이_내_정보를_조회하면_401이다() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_토큰으로_내_정보를_조회한다() throws Exception {
        String token = jwtProvider.createAccessToken(user.getId(), user.getRole());

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.nickname").value("현민"))
                .andExpect(jsonPath("$.role").value("FAN"))
                .andExpect(jsonPath("$.phoneVerified").value(false));
    }

    @Test
    void 만료된_토큰으로_조회하면_401이다() throws Exception {
        JwtProvider expiredIssuer = new JwtProvider(new JwtProperties(SECRET, -1, -1));
        String expiredToken = expiredIssuer.createAccessToken(user.getId(), user.getRole());

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 서명이_다른_토큰으로_조회하면_401이다() throws Exception {
        JwtProvider attacker = new JwtProvider(
                new JwtProperties("attacker-secret-key-also-32-bytes-long!!", 1800, 1209600));
        String forgedToken = attacker.createAccessToken(user.getId(), Role.HOST);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 재사용된_구_refresh_토큰은_거부한다() throws Exception {
        String oldToken = refreshTokenService.issue(user.getId());

        // 첫 재발급 — 새 access 토큰과 새 refresh 쿠키를 받는다
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", oldToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"));

        // 같은 토큰을 다시 쓰면 거부된다. rotation 이 첫 사용 때 폐기했기 때문이다.
        // 정상 흐름에서는 있을 수 없는 일이므로 탈취 신호로 볼 수 있다.
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", oldToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃하면_refresh_토큰이_거부된다() throws Exception {
        String token = refreshTokenService.issue(user.getId());

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", token)))
                .andExpect(status().isUnauthorized());
    }
}
