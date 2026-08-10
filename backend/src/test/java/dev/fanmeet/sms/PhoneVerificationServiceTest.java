package dev.fanmeet.sms;

import static dev.fanmeet.sms.PhoneVerificationService.MAX_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.fanmeet.user.Role;
import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PhoneVerificationServiceTest {

    private static final String PHONE = "01012345678";
    private static final String CODE = "483920";
    private static final Long USER_ID = 1L;

    private final PhoneVerificationRepository phoneVerificationRepository =
            mock(PhoneVerificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SmsSender smsSender = mock(SmsSender.class);

    private final PhoneVerificationService service =
            new PhoneVerificationService(phoneVerificationRepository, userRepository, smsSender);

    private final User user = User.builder()
            .provider("kakao").providerId("12345").nickname("현민").role(Role.FAN)
            .build();

    /** 발급 시각과 만료 시각을 직접 정해 "지금으로부터 몇 초 전에 발급된 코드"를 만든다. */
    private PhoneVerification 발급된(Instant issuedAt, Instant expiresAt) {
        return PhoneVerification.builder()
                .phone(PHONE).code(CODE).issuedAt(issuedAt).expiresAt(expiresAt)
                .build();
    }

    @Test
    void 인증번호가_맞으면_휴대폰_인증이_완료된다() {
        Instant now = Instant.now();
        given(phoneVerificationRepository.findByPhone(PHONE))
                .willReturn(Optional.of(발급된(now.minusSeconds(90), now.plusSeconds(200))));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        service.verify(USER_ID, PHONE, CODE);

        assertThat(user.isPhoneVerified()).isTrue();
        assertThat(user.getPhone()).isEqualTo(PHONE);
    }

    @Test
    void 만료된_인증번호는_정답이어도_거부한다() {
        Instant now = Instant.now();
        given(phoneVerificationRepository.findByPhone(PHONE))
                .willReturn(Optional.of(발급된(now.minusSeconds(400), now.minusSeconds(100))));

        assertThatThrownBy(() -> service.verify(USER_ID, PHONE, CODE))
                .isInstanceOf(InvalidVerificationCodeException.class);

        assertThat(user.isPhoneVerified()).isFalse();
    }

    @Test
    void 최대_횟수만큼_틀리면_정답을_넣어도_거부한다() {
        Instant now = Instant.now();
        given(phoneVerificationRepository.findByPhone(PHONE))
                .willReturn(Optional.of(발급된(now.minusSeconds(90), now.plusSeconds(200))));

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verify(USER_ID, PHONE, "000000"))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        // 6자리를 무차별 대입하지 못하게, 횟수를 다 쓰면 정답도 막는다
        assertThatThrownBy(() -> service.verify(USER_ID, PHONE, CODE))
                .isInstanceOf(InvalidVerificationCodeException.class);
        assertThat(user.isPhoneVerified()).isFalse();
    }

    @Test
    void 쿨다운_안에_재발송하면_문자를_보내지_않는다() {
        Instant now = Instant.now();
        given(phoneVerificationRepository.findByPhone(PHONE))
                .willReturn(Optional.of(발급된(now.minusSeconds(10), now.plusSeconds(290))));

        assertThatThrownBy(() -> service.request(PHONE))
                .isInstanceOf(ResendTooSoonException.class);

        // 발송 비용이 우리 부담이므로, 거부는 문자가 나가기 전에 이뤄져야 한다
        verify(smsSender, never()).send(any(), any());
    }
}
