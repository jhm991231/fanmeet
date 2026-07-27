package dev.fanmeet.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    private User kakaoUser(String providerId) {
        return User.builder()
                .provider("kakao")
                .providerId(providerId)
                .nickname("현민")
                .role(Role.FAN)
                .build();
    }

    @Test
    void provider와_providerId로_유저를_조회한다() {
        userRepository.save(kakaoUser("12345"));

        var found = userRepository.findByProviderAndProviderId("kakao", "12345");

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("현민");
        assertThat(found.get().isPhoneVerified()).isFalse();
    }

    @Test
    void 가입한_적_없는_유저는_빈_Optional을_돌려준다() {
        var found = userRepository.findByProviderAndProviderId("kakao", "99999");

        assertThat(found).isEmpty();
    }

    @Test
    void 같은_provider_providerId_조합은_두_번_저장할_수_없다() {
        userRepository.saveAndFlush(kakaoUser("12345"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(kakaoUser("12345")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
