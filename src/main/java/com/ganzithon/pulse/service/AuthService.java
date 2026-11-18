package com.ganzithon.pulse.service;

import com.ganzithon.pulse.dto.auth.AuthResponse;
import com.ganzithon.pulse.dto.auth.LoginRequest;
import com.ganzithon.pulse.dto.auth.SignupRequest;
import com.ganzithon.pulse.dto.auth.UserInfo;
import com.ganzithon.pulse.entity.User;
import com.ganzithon.pulse.global.auth.JwtProvider;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.repository.UserRepository;
import java.nio.charset.StandardCharsets;
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
        // BCrypt는 72바이트까지만 받는다. 한글은 글자당 3바이트라 글자 수 검증만으론 부족하다
        if (r.password().getBytes(StandardCharsets.UTF_8).length > SignupRequest.PASSWORD_MAX_BYTES) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
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
        // 가입 때 막은 길이라 맞는 비밀번호일 수 없다. BCrypt 예외(500) 대신 로그인 실패로 처리
        if (r.password().getBytes(StandardCharsets.UTF_8).length > SignupRequest.PASSWORD_MAX_BYTES) {
            throw new BusinessException(ErrorCode.AUTH_001);
        }
        User user = users.findByLoginId(r.loginId())
                .filter(u -> encoder.matches(r.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));
        return new AuthResponse(jwt.issue(user.getId()), UserInfo.from(user));
    }
}
