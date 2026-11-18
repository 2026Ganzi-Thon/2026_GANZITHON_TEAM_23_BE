package com.ganzithon.pulse.controller;

import com.ganzithon.pulse.dto.auth.AuthResponse;
import com.ganzithon.pulse.dto.auth.LoginRequest;
import com.ganzithon.pulse.dto.auth.SignupRequest;
import com.ganzithon.pulse.global.response.ApiResponse;
import com.ganzithon.pulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
