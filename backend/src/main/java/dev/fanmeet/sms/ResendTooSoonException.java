package dev.fanmeet.sms;

/**
 * 쿨다운이 끝나기 전에 재발송을 요청했다.
 *
 * <p>상태 코드는 {@code ApiExceptionHandler}가 정한다(429).
 * 429는 "지금은 안 되지만 기다리면 된다"는 뜻이라 안내 문구를 띄우기 좋다.
 */
public class ResendTooSoonException extends RuntimeException {

    public ResendTooSoonException(String message) {
        super(message);
    }
}
