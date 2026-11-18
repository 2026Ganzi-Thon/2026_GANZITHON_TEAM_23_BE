package com.ganzithon.pulse.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 20, message = "이름은 20자 이내예요.") String name,
        @NotBlank(message = "닉네임을 입력해 주세요.") @Size(max = 10, message = "닉네임은 10자 이내예요.") String nickname,
        @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 255, message = "이메일은 255자 이내예요.") String email,
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "아이디는 영문·숫자 4~20자예요.") String loginId,
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = PASSWORD_MIN, max = PASSWORD_MAX_BYTES,
                message = "비밀번호는 " + PASSWORD_MIN + "자 이상 " + PASSWORD_MAX_BYTES + "자 이하예요.") String password) {

    /** 비밀번호 최소 길이. 팀 결정(2026-11-18): 8자 이상 */
    public static final int PASSWORD_MIN = 8;
    /** BCrypt는 UTF-8 기준 72바이트까지만 받는다. 한글 등 멀티바이트는 서비스에서 바이트 길이로 다시 확인 */
    public static final int PASSWORD_MAX_BYTES = 72;

    public SignupRequest {
        name = strip(name);
        nickname = strip(nickname);
        email = strip(email);
        loginId = strip(loginId);
    }

    private static String strip(String s) {
        return s == null ? null : s.strip();
    }
}
