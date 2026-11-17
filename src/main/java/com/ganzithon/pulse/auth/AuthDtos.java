package com.ganzithon.pulse.auth;

import com.ganzithon.pulse.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    /** 비밀번호 최소 길이. 피그마는 10자, 명세서 v0.1은 8자라 팀 확정 후 여기만 바꾸면 됨 */
    public static final int PASSWORD_MIN = 8;

    public record SignupRequest(
            @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 20, message = "이름은 20자 이내예요.") String name,
            @NotBlank(message = "닉네임을 입력해 주세요.") @Size(max = 10, message = "닉네임은 10자 이내예요.") String nickname,
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 아닙니다.") String email,
            @NotBlank(message = "아이디를 입력해 주세요.")
            @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "아이디는 영문·숫자 4~20자예요.") String loginId,
            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Size(min = PASSWORD_MIN, message = "비밀번호는 " + PASSWORD_MIN + "자 이상이에요.") String password) {

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

    public record LoginRequest(
            @NotBlank(message = "아이디를 입력해 주세요.") String loginId,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {}

    public record UserInfo(Long userId, String name, String nickname, String email, String loginId) {
        static UserInfo from(User u) {
            return new UserInfo(u.getId(), u.getName(), u.getNickname(), u.getEmail(), u.getLoginId());
        }
    }

    public record AuthResponse(String accessToken, UserInfo user) {}
}
