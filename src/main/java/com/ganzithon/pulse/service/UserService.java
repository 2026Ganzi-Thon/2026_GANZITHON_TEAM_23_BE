package com.ganzithon.pulse.service;

import com.ganzithon.pulse.dto.user.GuardianRequest;
import com.ganzithon.pulse.dto.user.GuardianResponse;
import com.ganzithon.pulse.dto.user.MeResponse;
import com.ganzithon.pulse.dto.user.UpdateMeRequest;
import com.ganzithon.pulse.entity.User;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        return MeResponse.from(find(userId));
    }

    @Transactional
    public MeResponse updateMe(Long userId, UpdateMeRequest r) {
        User user = find(userId);
        user.updateProfile(r.name(), r.nickname(), r.email());
        return MeResponse.from(user);
    }

    @Transactional
    public GuardianResponse putGuardian(Long userId, GuardianRequest r) {
        User user = find(userId);
        user.assignGuardian(r.name(), r.phone());
        return GuardianResponse.from(user);
    }

    /** 보호자가 없어도 성공으로 처리한다 */
    @Transactional
    public void deleteGuardian(Long userId) {
        find(userId).clearGuardian();
    }

    /** 토큰은 유효한데 사용자가 없으면(예: 로컬 서버 재시작) 다시 로그인하게 한다 */
    private User find(Long userId) {
        return users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_003));
    }
}
