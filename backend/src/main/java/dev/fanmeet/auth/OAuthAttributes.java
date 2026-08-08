package dev.fanmeet.auth;

import java.util.Map;

/**
 * 제공자마다 제각각인 사용자 정보 응답을 우리가 쓰는 공통 형태로 변환한다.
 * - 카카오: { id: 123, kakao_account: { profile: { nickname: "..." } } }
 * - 구글:   { sub: "1179...", name: "...", email: "..." }  ← 중첩이 없다
 */
public record OAuthAttributes(String provider, String providerId, String nickname) {

    public static OAuthAttributes from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao" -> ofKakao(attributes);
            case "google" -> ofGoogle(attributes);
            case "naver" -> ofNaver(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 로그인 제공자: " + registrationId);
        };
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        return new OAuthAttributes(
                "kakao",
                String.valueOf(attributes.get("id")),
                (String) profile.get("nickname"));
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        // 실제 정보는 response 아래에 한 겹 더 들어 있다. 바깥에는 resultcode/message만 있다.
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return new OAuthAttributes(
                "naver",
                (String) response.get("id"),
                (String) response.get("nickname"));
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        // sub는 구글이 앱별로 부여하는 불변 식별자다. email은 바뀔 수 있으므로 키로 쓰지 않는다.
        return new OAuthAttributes(
                "google",
                (String) attributes.get("sub"),
                (String) attributes.get("name"));
    }
}
