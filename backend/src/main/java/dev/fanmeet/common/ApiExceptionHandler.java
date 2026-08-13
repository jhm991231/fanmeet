package dev.fanmeet.common;

import dev.fanmeet.sms.InvalidVerificationCodeException;
import dev.fanmeet.sms.ResendTooSoonException;
import dev.fanmeet.sms.SmsSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 사용자에게 보여줄 실패를 응답 본문으로 바꾼다.
 *
 * <p>{@code @ResponseStatus}만으로는 상태 코드밖에 못 준다. 스프링은 예외 메시지를 기본적으로
 * 본문에서 지우는데(내부 정보 유출 방지), 그러면 만료·오답·횟수초과가 전부 400 하나로 뭉개진다.
 * 여기 등록한 예외만 메시지를 통과시켜 그 구분을 살린다.
 *
 * <p>등록하지 않은 예외는 종전대로 메시지 없이 나간다. 그게 기본값이어야 한다 —
 * 새 예외를 만들 때마다 "이 문구를 사용자가 봐도 되는가"를 한 번 거치게 된다.
 *
 * <p>로그 정책: 사용자가 스스로 고칠 수 있는 실패(오답·만료·쿨다운·입력값)는 남기지 않는다.
 * 정상적인 사용 중에 늘 생기는 일이라 로그만 채운다. 사용자가 손쓸 수 없는 실패만 남긴다.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidVerificationCodeException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ResendTooSoonException.class)
    public ResponseEntity<ErrorResponse> handle(ResendTooSoonException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(e.getMessage()));
    }

    /**
     * 외부 문자 업체 호출 실패. 상대 쪽 문제이므로 502 — 우리 버그(500)와 구분한다.
     *
     * <p>여기만 error로 남긴다. 사용자가 할 수 있는 게 없는 실패라 우리가 봐야 한다.
     * {@code e}를 두 번째 인자로 넘겨야 {@code Caused by:}로 원인(401인지 타임아웃인지)이
     * 함께 찍힌다. {@code "..." + e}나 {@code {}}로 넣으면 문자열로 바뀌어 사슬이 사라진다.
     */
    @ExceptionHandler(SmsSendException.class)
    public ResponseEntity<ErrorResponse> handle(SmsSendException e) {
        log.error("문자 발송 실패", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(e.getMessage()));
    }

    /** @Valid 실패. 첫 번째 위반 메시지만 보낸다 — 사용자는 한 번에 하나씩 고친다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
