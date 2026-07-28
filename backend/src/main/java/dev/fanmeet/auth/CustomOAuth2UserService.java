package dev.fanmeet.auth;

import dev.fanmeet.user.Role;
import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카카오에서 사용자 정보를 받아온 직후 호출된다.
 * 우리 DB에 유저를 upsert하고, 이후 단계(SuccessHandler)가 쓸 principal을 만든다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest); // 카카오 user-info API 호출은 부모가 처리

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthAttributes attributes = OAuthAttributes.from(registrationId, oAuth2User.getAttributes());
        User user = upsert(attributes);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                Map.of("userId", user.getId(), "role", user.getRole().name()),
                "userId");
    }

    User upsert(OAuthAttributes attributes) {
        return userRepository.findByProviderAndProviderId(attributes.provider(), attributes.providerId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .provider(attributes.provider())
                        .providerId(attributes.providerId())
                        .nickname(attributes.nickname())
                        .role(Role.FAN)
                        .build()));
    }
}
