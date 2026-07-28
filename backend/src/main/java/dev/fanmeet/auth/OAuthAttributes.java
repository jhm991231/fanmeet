package dev.fanmeet.auth;

import java.util.Map;

/**
 * 제공자마다 제각각인 사용자 정보 응답을 우리가 쓰는 공통 형태로 변환한다.
 * 카카오 응답 구조: { id: 123, kakao_account: { profile: { nickname: "..." } } }
 */
public record OAuthAttributes(String provider, String providerId, String nickname) {

    @SuppressWarnings("unchecked")
    public static OAuthAttributes from(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            return new OAuthAttributes(
                    "kakao",
                    String.valueOf(attributes.get("id")),
                    (String) profile.get("nickname"));
        }
        throw new IllegalArgumentException("지원하지 않는 로그인 제공자: " + registrationId);
    }
}
