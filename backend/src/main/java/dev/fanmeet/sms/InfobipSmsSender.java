package dev.fanmeet.sms;

import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Infobip REST API로 실제 문자를 보낸다. {@code sms} 프로파일에서만 뜬다.
 *
 * <p>여기가 우리 애플리케이션이 <b>클라이언트가 되는</b> 유일한 곳이다. HTTP·API 키·타임아웃·
 * 국가번호 변환이 전부 이 파일 안에 갇혀 있고, {@code PhoneVerificationService}는 그중
 * 아무것도 모른다.
 */
@Component
@Profile("sms")
public class InfobipSmsSender implements SmsSender {

    private static final String SEND_PATH = "/sms/3/messages";

    private final InfobipProperties properties;
    private final RestClient restClient;

    public InfobipSmsSender(InfobipProperties properties) {
        this.properties = properties;

        // 타임아웃이 없으면 상대가 응답하지 않을 때 스레드가 하나씩 묶이다 서버 전체가 멈춘다.
        // 사용자는 이 응답을 기다리고 있으므로 짧게 잡는다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        // RestClient.Builder를 주입받지 않고 직접 만든다. 부트가 자동 설정해주는 빌더는
        // 별도 모듈에 있어 지금 클래스패스에 없다. 우리가 보내는 본문은 문자열과 목록뿐이라
        // 기본 ObjectMapper로 충분하다.
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public void send(String phone, String message) {
        SendRequest body = new SendRequest(List.of(
                new Message(
                        List.of(new Destination(toInternational(phone))),
                        properties.sender(),
                        new Content(message))));

        try {
            restClient.post()
                    .uri(SEND_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "App " + properties.apiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity(); // 응답 본문(messageId 등)은 지금 쓸 곳이 없다
        } catch (RestClientException e) {
            // 상대 잘못이지 우리 버그가 아니다. SmsSendException은 502로 나간다.
            throw new SmsSendException("문자 발송에 실패했습니다", e);
        }
    }

    /**
     * 국내 표기를 국제 표기로. {@code 01012345678 → 821012345678}
     *
     * <p>이 변환이 여기 있는 이유: 국가번호는 Infobip의 요구사항이지 우리 도메인의 개념이 아니다.
     * 서비스와 컨트롤러는 계속 국내 표기 한 가지만 안다.
     */
    private String toInternational(String phone) {
        return "82" + phone.substring(1);
    }

    // Infobip 요청 본문의 모양(SMS API v3). 중첩 구조를 record로 적어두면 JSON이 그대로 보인다.
    // { "messages": [ { "destinations": [ { "to": "..." } ],
    //                   "sender": "...", "content": { "text": "..." } } ] }
    record SendRequest(List<Message> messages) {
    }

    record Message(List<Destination> destinations, String sender, Content content) {
    }

    record Destination(String to) {
    }

    record Content(String text) {
    }
}
