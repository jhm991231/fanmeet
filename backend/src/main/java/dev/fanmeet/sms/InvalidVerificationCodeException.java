package dev.fanmeet.sms;

/**
 * 인증번호가 틀렸거나, 만료됐거나, 시도 횟수를 다 썼다.
 * 셋을 한 예외로 묶고 메시지로 구분한다 — 클라이언트가 할 일은 셋 다 "다시 받기"로 같기 때문.
 *
 * <p>상태 코드는 {@code ApiExceptionHandler}가 정한다(400).
 */
public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
