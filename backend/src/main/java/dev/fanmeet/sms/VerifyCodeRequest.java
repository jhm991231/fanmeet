package dev.fanmeet.sms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
        String phone,

        @NotBlank(message = "인증번호를 입력해주세요")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자입니다")
        String code) {
}
