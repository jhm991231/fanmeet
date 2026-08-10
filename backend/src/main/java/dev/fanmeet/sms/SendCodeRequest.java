package dev.fanmeet.sms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 하이픈 없이 숫자만 받는다. 표기 정규화는 프론트가 하고, 서버는 한 가지 형태만 안다. */
public record SendCodeRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
        String phone) {
}
