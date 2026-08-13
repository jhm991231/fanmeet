package dev.fanmeet.sms;

import static dev.fanmeet.sms.PhoneVerificationService.MAX_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.fanmeet.user.Role;
import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@code @Transactional(noRollbackFor = ...)}가 실제로 동작하는지 확인한다.
 *
 * <p><b>왜 따로 있나.</b> {@link PhoneVerificationServiceTest}의 시도 제한 테스트는 목이 매번
 * 같은 {@link PhoneVerification} 인스턴스를 돌려줘서 메모리에서 횟수가 쌓인다. 그래서
 * {@code noRollbackFor}를 지워도 통과한다 — 규칙을 검증하는 게 아니라 목의 동작을 검증하고 있다.
 * 진짜 트랜잭션과 진짜 DB가 있어야만 "롤백돼서 증가분이 사라지는가"를 물을 수 있다.
 *
 * <p><b>왜 이 클래스에 {@code @Transactional}을 붙이면 안 되나.</b> 테스트에 붙이면 서비스의
 * 트랜잭션이 바깥 트랜잭션에 합류해버려서 커밋 시점이 테스트 끝으로 밀린다. 그러면 검증하려던
 * 경계 자체가 사라진다. 게다가 테스트가 끝나며 전부 롤백되므로 DB에 뭐가 남았는지 볼 수도 없다.
 * 대신 {@code @AfterEach}에서 직접 지운다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PhoneVerificationRollbackTest {

    private static final String PHONE = "01012345678";

    @Autowired
    PhoneVerificationService phoneVerificationService;

    @Autowired
    PhoneVerificationRepository phoneVerificationRepository;

    @Autowired
    UserRepository userRepository;

    /** test 프로파일에는 SmsSender 구현이 없다(local도 sms도 아니다). 발송은 이 테스트의 관심사가 아니다. */
    @MockitoBean
    SmsSender smsSender;

    private Long userId;

    @BeforeEach
    void 유저와_인증번호를_준비한다() {
        userId = userRepository.save(User.builder()
                .provider("kakao").providerId("12345").nickname("현민").role(Role.FAN)
                .build()).getId();
        phoneVerificationService.request(PHONE);
    }

    @AfterEach
    void 정리한다() {
        phoneVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    /** 서비스가 무작위로 만든 코드라 테스트가 알 수 없다. DB에서 읽어온다. */
    private String 발급된_코드() {
        return phoneVerificationRepository.findByPhone(PHONE).orElseThrow().getCode();
    }

    /** 우연히 정답과 같아지지 않도록 발급된 코드를 보고 고른다. */
    private String 틀린_코드() {
        return 발급된_코드().equals("000000") ? "111111" : "000000";
    }

    private int DB에_기록된_시도_횟수() {
        return phoneVerificationRepository.findByPhone(PHONE).orElseThrow().getAttemptCount();
    }

    @Test
    void 인증번호가_틀리면_시도_횟수가_DB에_남는다() {
        assertThatThrownBy(() -> phoneVerificationService.verify(userId, PHONE, 틀린_코드()))
                .isInstanceOf(InvalidVerificationCodeException.class);

        // noRollbackFor 가 없으면 예외와 함께 증가분이 롤백돼 0 이 된다
        assertThat(DB에_기록된_시도_횟수()).isEqualTo(1);
    }

    @Test
    void 최대_횟수를_다_쓰면_정답도_거부한다() {
        String 틀린_코드 = 틀린_코드();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> phoneVerificationService.verify(userId, PHONE, 틀린_코드))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }
        assertThat(DB에_기록된_시도_횟수()).isEqualTo(MAX_ATTEMPTS);

        // 횟수가 쌓이지 않으면 여기서 통과해버린다 = 6자리를 무제한 대입할 수 있다
        assertThatThrownBy(() -> phoneVerificationService.verify(userId, PHONE, 발급된_코드()))
                .isInstanceOf(InvalidVerificationCodeException.class);
        assertThat(userRepository.findById(userId).orElseThrow().isPhoneVerified()).isFalse();
    }
}
