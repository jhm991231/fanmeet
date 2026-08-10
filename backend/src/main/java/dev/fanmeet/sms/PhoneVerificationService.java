package dev.fanmeet.sms;

import dev.fanmeet.user.User;
import dev.fanmeet.user.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 휴대폰 인증의 규칙을 모아둔 곳. 발송 수단은 {@link SmsSender}에 맡기고 여기서는 모른다.
 *
 * <p>규칙 셋은 각각 다른 공격을 막는다.
 * <ul>
 *   <li>유효시간 — 예전에 받은 문자를 나중에 주워 쓰는 것</li>
 *   <li>쿨다운 — 남의 번호로 문자 폭탄. 발송 비용이 우리 부담이라 더 중요하다</li>
 *   <li>시도 제한 — 6자리(100만 가지)를 자동화로 대입하는 것</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    static final Duration VALIDITY = Duration.ofMinutes(5);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;

    @Transactional
    public void request(String phone) {
        Instant now = Instant.now();
        PhoneVerification existing = phoneVerificationRepository.findByPhone(phone).orElse(null);

        // 거부는 문자가 나가기 전에. 발송한 뒤에 막으면 비용은 이미 발생한 뒤다.
        if (existing != null && existing.isCoolingDown(now, RESEND_COOLDOWN)) {
            throw new ResendTooSoonException(
                    "인증번호는 %d초에 한 번만 보낼 수 있습니다".formatted(RESEND_COOLDOWN.toSeconds()));
        }

        String code = generateCode();
        if (existing != null) {
            existing.reissue(code, now, now.plus(VALIDITY)); // 만료된 행도 여기서 정리된다
        } else {
            phoneVerificationRepository.save(PhoneVerification.builder()
                    .phone(phone)
                    .code(code)
                    .issuedAt(now)
                    .expiresAt(now.plus(VALIDITY))
                    .build());
        }

        smsSender.send(phone,
                "[fanmeet] 인증번호 %s (%d분 안에 입력)".formatted(code, VALIDITY.toMinutes()));
    }

    /**
     * 틀렸을 때 시도 횟수를 올리고 예외를 던지는데, 기본 설정이면 그 예외가 트랜잭션을
     * 롤백시켜 증가분이 사라진다. 그러면 시도 제한이 영원히 걸리지 않는다.
     * 이 예외만 롤백 대상에서 뺀다.
     */
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public void verify(Long userId, String phone, String code) {
        PhoneVerification verification = phoneVerificationRepository.findByPhone(phone)
                .orElseThrow(() -> new InvalidVerificationCodeException("발송된 인증번호가 없습니다"));

        // 잠금을 먼저 본다. 횟수를 다 쓴 뒤에는 정답이든 오답이든 더 볼 것이 없다.
        if (verification.isLockedOut(MAX_ATTEMPTS)) {
            throw new InvalidVerificationCodeException("시도 횟수를 초과했습니다. 다시 받아주세요");
        }
        // 만료를 대조보다 먼저 본다. 어차피 실패할 시도로 남은 횟수를 깎지 않는다.
        if (verification.isExpired(Instant.now())) {
            throw new InvalidVerificationCodeException("만료된 인증번호입니다. 다시 받아주세요");
        }
        if (!verification.matches(code)) {
            verification.recordFailure();
            throw new InvalidVerificationCodeException("인증번호가 일치하지 않습니다");
        }

        // 토큰이 살아 있는데 유저가 없다면 우리 쪽 정합성 문제다. 사용자 입력 오류가 아니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + userId));
        user.verifyPhone(phone);

        // 같은 코드로 두 번 통과시키지 않는다
        phoneVerificationRepository.delete(verification);
    }

    /** 6자리. 앞자리가 0이어도 자릿수를 유지한다. */
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
