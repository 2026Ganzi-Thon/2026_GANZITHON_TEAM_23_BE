package com.ganzithon.pulse.dto.user;

import com.ganzithon.pulse.entity.User;

public record MeResponse(Long userId, String name, String nickname, String email, String loginId,
                         GuardianResponse guardian) {
    public static MeResponse from(User u) {
        return new MeResponse(u.getId(), u.getName(), u.getNickname(), u.getEmail(), u.getLoginId(),
                GuardianResponse.from(u));
    }
}
