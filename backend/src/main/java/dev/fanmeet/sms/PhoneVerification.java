package dev.fanmeet.sms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발송한 인증번호 한 건. 번호당 한 행만 두고 재발송 때 갈아끼운다.
 *
 * <p>시각을 스스로 구하지 않고 밖에서 받는다. 그래야 "5분 전에 발급된 코드" 같은 상태를
 * 테스트에서 만들 수 있다. 정책(유효시간·쿨다운·최대 시도)은 서비스가 쥐고, 엔티티는
 * 주어진 값으로 판정만 한다.
 */
@Entity
@Table(
        name = "phone_verifications",
        uniqueConstraints = @UniqueConstraint(columnNames = "phone")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attemptCount;

    @Builder
    private PhoneVerification(String phone, String code, Instant issuedAt, Instant expiresAt) {
        this.phone = phone;
        this.code = code;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }

    /**
     * 재발송. 행을 새로 만들지 않고 덮어쓴다. 시도 횟수도 0으로 돌아간다.
     * 만료된 행도 이 경로로 자연히 정리되므로 별도 정리 작업이 필요 없다.
     */
    void reissue(String code, Instant issuedAt, Instant expiresAt) {
        this.code = code;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }

    boolean isCoolingDown(Instant now, Duration cooldown) {
        return now.isBefore(issuedAt.plus(cooldown));
    }

    boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    /** 시도 횟수를 다 쓰면 정답을 넣어도 통과시키지 않는다. */
    boolean isLockedOut(int maxAttempts) {
        return attemptCount >= maxAttempts;
    }

    boolean matches(String input) {
        return code.equals(input);
    }

    void recordFailure() {
        attemptCount++;
    }
}
