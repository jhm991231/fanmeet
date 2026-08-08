package dev.fanmeet.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발용 구현체. 실제로 보내지 않고 로그에만 남긴다.
 * 인증번호는 콘솔에서 눈으로 읽어 입력창에 넣는다.
 *
 * <p>개발 중에는 "인증번호 받기"를 수십 번 누르게 되는데, 실발송이면 그대로 과금된다.
 * 문자 도착을 기다릴 필요도 없어 확인이 빠르다.
 */
@Slf4j
@Component
@Profile("local")
public class ConsoleSmsSender implements SmsSender {

    @Override
    public void send(String phone, String message) {
        // Hibernate SQL 로그에 묻히지 않도록 구분선을 준다
        log.info("""

                ┌─ SMS ─────────────────────────────
                │ 수신 {}
                │ 내용 {}
                └───────────────────────────────────""", phone, message);
    }
}
