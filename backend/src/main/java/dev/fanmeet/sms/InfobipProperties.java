package dev.fanmeet.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * yml의 sms.infobip.* 설정 묶음.
 *
 * <p>{@code baseUrl}은 계정마다 다르다(예: {@code https://xxxxx.api.infobip.com}).
 * Infobip 대시보드 상단이나 API 키 발급 화면에서 확인한다.
 *
 * <p>{@code sender}는 발신자 표시명. 체험 계정은 지정한 값이 무시되고
 * 고정 문구로 나갈 수 있다.
 */
@ConfigurationProperties(prefix = "sms.infobip")
public record InfobipProperties(
        String baseUrl,
        String apiKey,
        String sender) {
}
