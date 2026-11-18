package com.ganzithon.pulse.dto.user;

import com.ganzithon.pulse.entity.User;

public record GuardianResponse(String name, String phone) {
    /** 보호자가 없으면 null */
    public static GuardianResponse from(User u) {
        if (u.getGuardianName() == null) return null;
        return new GuardianResponse(u.getGuardianName(), u.getGuardianPhone());
    }
}
