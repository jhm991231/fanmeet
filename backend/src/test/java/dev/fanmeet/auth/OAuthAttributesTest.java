package dev.fanmeet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuthAttributesTest {

    @Test
    void 카카오_응답에서_id와_닉네임을_꺼낸다() {
        Map<String, Object> kakaoResponse = Map.of(
                "id", 12345L,
                "kakao_account", Map.of("profile", Map.of("nickname", "현민")));

        OAuthAttributes attributes = OAuthAttributes.from("kakao", kakaoResponse);

        assertThat(attributes.provider()).isEqualTo("kakao");
        assertThat(attributes.providerId()).isEqualTo("12345");
        assertThat(attributes.nickname()).isEqualTo("현민");
    }

    @Test
    void 구글_응답에서_sub와_이름을_꺼낸다() {
        // 카카오와 달리 중첩이 없다. sub는 구글이 부여한 고유 식별자.
        Map<String, Object> googleResponse = Map.of(
                "sub", "117905123456789",
                "name", "정현민",
                "email", "jhm991231@gmail.com");

        OAuthAttributes attributes = OAuthAttributes.from("google", googleResponse);

        assertThat(attributes.provider()).isEqualTo("google");
        assertThat(attributes.providerId()).isEqualTo("117905123456789");
        assertThat(attributes.nickname()).isEqualTo("정현민");
    }

    @Test
    void 네이버_응답에서_response_안의_id와_닉네임을_꺼낸다() {
        // 네이버는 실제 정보가 response 아래에 한 겹 더 들어 있다.
        Map<String, Object> naverResponse = Map.of(
                "resultcode", "00",
                "message", "success",
                "response", Map.of("id", "32742776", "nickname", "현민"));

        OAuthAttributes attributes = OAuthAttributes.from("naver", naverResponse);

        assertThat(attributes.provider()).isEqualTo("naver");
        assertThat(attributes.providerId()).isEqualTo("32742776");
        assertThat(attributes.nickname()).isEqualTo("현민");
    }

    @Test
    void 모르는_제공자면_예외가_난다() {
        assertThatThrownBy(() -> OAuthAttributes.from("facebook", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
