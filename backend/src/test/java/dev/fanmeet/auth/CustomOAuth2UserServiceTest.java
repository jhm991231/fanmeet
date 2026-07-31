package dev.fanmeet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.fanmeet.user.Role;
import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomOAuth2UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository);

    private final OAuthAttributes kakaoAttributes = new OAuthAttributes("kakao", "12345", "현민");

    @Test
    void 처음_로그인한_유저는_FAN으로_가입된다() {
        given(userRepository.findByProviderAndProviderId("kakao", "12345")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        User user = service.upsert(kakaoAttributes);

        assertThat(user.getNickname()).isEqualTo("현민");
        assertThat(user.getRole()).isEqualTo(Role.FAN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 이미_가입된_유저는_새로_저장하지_않고_그대로_돌려준다() {
        User existing = User.builder()
                .provider("kakao").providerId("12345").nickname("현민").role(Role.HOST)
                .build();
        given(userRepository.findByProviderAndProviderId("kakao", "12345")).willReturn(Optional.of(existing));

        User user = service.upsert(kakaoAttributes);

        assertThat(user).isSameAs(existing);
        assertThat(user.getRole()).isEqualTo(Role.HOST); // 기존 역할이 FAN으로 덮어써지지 않는다
        verify(userRepository, never()).save(any(User.class));
    }
}
