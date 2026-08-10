package dev.fanmeet.common;

/** 클라이언트에 보여줘도 되는 실패 사유. 내부 예외 메시지를 그대로 흘리지 않는다. */
public record ErrorResponse(String message) {
}
