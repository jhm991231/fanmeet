package dev.fanmeet.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * refresh 토큰이 없거나, 만료됐거나, 이미 사용돼 폐기된 경우.
 *
 * <p>여기까지 오면 자동 복구 수단이 없다. 클라이언트는 재로그인해야 한다.
 *
 * <p>세 경우를 구분해 알리지 않는다. 어느 쪽이든 클라이언트가 할 일은 같고, 공격자에게
 * "토큰은 있는데 만료됐다" 같은 힌트를 줄 이유도 없다.
 *
 * <p>{@code @ResponseStatus}로 401을 직접 지정한다. 계획서 Task 15에서 전역 예외 처리를
 * 도입하면 그쪽으로 옮길 자리다.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
