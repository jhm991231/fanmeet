package dev.fanmeet.sms;

/**
 * 문자 발송 창구. "무엇을 보낸다"만 정의하고 "어떤 업체로 보낸다"는 모른다.
 *
 * <p>구현체는 활성 프로파일이 고른다.
 * <ul>
 *   <li>{@code local} — {@link ConsoleSmsSender}: 로그에만 출력. 과금 없음</li>
 *   <li>{@code sms} — CoolSMS 실발송</li>
 * </ul>
 *
 * <p>발송에 실패하면 {@link SmsSendException}을 던진다.
 */
public interface SmsSender {

    void send(String phone, String message);
}
