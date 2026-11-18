package com.ganzithon.pulse.controller;

import com.ganzithon.pulse.dto.user.GuardianRequest;
import com.ganzithon.pulse.dto.user.GuardianResponse;
import com.ganzithon.pulse.dto.user.MeResponse;
import com.ganzithon.pulse.dto.user.UpdateMeRequest;
import com.ganzithon.pulse.global.auth.AuthInterceptor;
import com.ganzithon.pulse.global.response.ApiResponse;
import com.ganzithon.pulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<MeResponse> get(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return ApiResponse.ok(userService.getMe(userId));
    }

    @PatchMapping
    public ApiResponse<MeResponse> update(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                          @Valid @RequestBody UpdateMeRequest request) {
        return ApiResponse.ok(userService.updateMe(userId, request));
    }

    @PutMapping("/guardian")
    public ApiResponse<GuardianResponse> putGuardian(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                                     @Valid @RequestBody GuardianRequest request) {
        return ApiResponse.ok(userService.putGuardian(userId, request));
    }

    @DeleteMapping("/guardian")
    public ApiResponse<Void> deleteGuardian(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        userService.deleteGuardian(userId);
        return ApiResponse.message("처리되었습니다.");
    }
}
