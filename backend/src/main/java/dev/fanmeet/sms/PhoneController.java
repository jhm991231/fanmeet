package dev.fanmeet.sms;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 휴대폰 인증 발송/검증.
 *
 * <p>둘 다 로그인한 사용자만 부를 수 있다({@code anyRequest().authenticated()}). 발송에도
 * 인증을 요구하는 이유는, 열어두면 누구나 남의 번호로 문자를 쏘게 만들 수 있고
 * 그 비용이 우리 부담이기 때문이다. 쿨다운은 그다음 방어선이다.
 */
@RestController
@RequestMapping("/api/phone")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/request")
    public void request(@Valid @RequestBody SendCodeRequest request) {
        phoneVerificationService.request(request.phone());
    }

    @PostMapping("/verify")
    public void verify(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyCodeRequest request) {
        phoneVerificationService.verify(userId, request.phone(), request.code());
    }
}
