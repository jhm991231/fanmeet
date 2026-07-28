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
    void 모르는_제공자면_예외가_난다() {
        assertThatThrownBy(() -> OAuthAttributes.from("naver", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
