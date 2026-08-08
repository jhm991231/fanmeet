package dev.fanmeet.sms;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 문자 발송 실패. 우리 잘못이 아니라 외부 업체 쪽 문제이므로 502로 답한다.
 * (500으로 두면 우리 버그와 구분이 안 되고, 클라이언트가 재시도해도 되는지 판단할 수 없다)
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class SmsSendException extends RuntimeException {

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
