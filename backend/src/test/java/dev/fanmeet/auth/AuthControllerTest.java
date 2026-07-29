package dev.fanmeet.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fanmeet.user.Role;
import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String TEST_SECRET = "fanmeet-test-secret-key-must-be-32-bytes!";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtProvider jwtProvider;

    private User user;

    @BeforeEach
    void setUp() {
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
        JwtProvider expiredIssuer = new JwtProvider(new JwtProperties(TEST_SECRET, -1, -1));
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
}
