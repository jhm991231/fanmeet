package dev.fanmeet.sms;

/**
 * 문자 발송 실패. 우리 잘못이 아니라 외부 업체 쪽 문제이므로 502로 답한다.
 * (500으로 두면 우리 버그와 구분이 안 되고, 클라이언트가 재시도해도 되는지 판단할 수 없다)
 *
 * <p>상태 코드는 {@code ApiExceptionHandler}가 정한다(502). 나머지 두 예외와 같은 자리에 둬야
 * 상태 코드가 한 파일에 모이고, 무엇보다 <b>로그를 남길 자리</b>가 생긴다. {@code @ResponseStatus}로
 * 두면 스프링이 "처리됨"으로 보고 스택 트레이스를 찍지 않아, 외부 연동이 왜 실패했는지가
 * 아무 데도 남지 않는다.
 */
public class SmsSendException extends RuntimeException {

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
