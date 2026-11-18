package com.ganzithon.pulse.dto.auth;

public record AuthResponse(String accessToken, UserInfo user) {}
