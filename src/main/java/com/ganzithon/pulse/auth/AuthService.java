package com.ganzithon.pulse.auth;

import com.ganzithon.pulse.auth.AuthDtos.*;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.user.User;
import com.ganzithon.pulse.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtProvider jwt;
    private final int baseThreshold;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtProvider jwt,
                       @Value("${pulse.alert.base-threshold}") int baseThreshold) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.baseThreshold = baseThreshold;
    }

    @Transactional
    public AuthResponse signup(SignupRequest r) {
        if (users.existsByLoginId(r.loginId())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        try {
            User saved = users.saveAndFlush(new User(r.name(), r.nickname(), r.email(), r.loginId(),
                    encoder.encode(r.password()), baseThreshold));
            return new AuthResponse(jwt.issue(saved.getId()), UserInfo.from(saved));
        } catch (DataIntegrityViolationException e) { // 동시 가입 경합
            throw new BusinessException(ErrorCode.AUTH_002);
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User user = users.findByLoginId(r.loginId())
                .filter(u -> encoder.matches(r.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));
        return new AuthResponse(jwt.issue(user.getId()), UserInfo.from(user));
    }
}
