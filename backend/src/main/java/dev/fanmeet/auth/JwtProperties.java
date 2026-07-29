package dev.fanmeet.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * yml의 jwt.* 설정 묶음. 흩어져 있던 @Value 세 개를 타입 있는 객체 하나로 대체한다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessValiditySeconds,
        long refreshValiditySeconds) {
}
