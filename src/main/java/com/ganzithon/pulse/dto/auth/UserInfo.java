package com.ganzithon.pulse.dto.auth;

import com.ganzithon.pulse.entity.User;

public record UserInfo(Long userId, String name, String nickname, String email, String loginId) {
    public static UserInfo from(User u) {
        return new UserInfo(u.getId(), u.getName(), u.getNickname(), u.getEmail(), u.getLoginId());
    }
}
