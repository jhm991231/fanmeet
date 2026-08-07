package dev.fanmeet.auth;

/**
 * 재발급 응답. refresh 토큰은 본문에 담지 않는다 — HttpOnly 쿠키로만 나간다.
 */
public record TokenResponse(String accessToken) {
}
