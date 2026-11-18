package com.ganzithon.pulse.dto.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** 보낸 필드만 바꾼다. null이면 바꾸지 않는다 (세 열이 모두 NOT NULL이라 값을 지우는 요청은 없다). */
public record UpdateMeRequest(
        @Size(min = 1, max = 20, message = "이름은 1~20자예요.") String name,
        @Size(min = 1, max = 10, message = "닉네임은 1~10자예요.") String nickname,
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(min = 1, max = 255, message = "이메일은 1~255자예요.") String email) {

    public UpdateMeRequest {
        name = strip(name);
        nickname = strip(nickname);
        email = strip(email);
    }

    @AssertTrue(message = "바꿀 항목을 하나 이상 보내 주세요.")
    public boolean isAnyFieldPresent() {
        return name != null || nickname != null || email != null;
    }

    private static String strip(String s) {
        return s == null ? null : s.strip();
    }
}
