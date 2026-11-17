package com.ganzithon.pulse.alert;

import com.ganzithon.pulse.alert.AlertDtos.*;
import com.ganzithon.pulse.auth.AuthInterceptor;
import com.ganzithon.pulse.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/heart-rate/spikes")
    public ApiResponse<SpikeResponse> spike(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                            @Valid @RequestBody SpikeRequest request) {
        return ApiResponse.ok(alertService.report(userId, request));
    }

    @PostMapping("/alerts/{alertId}/dismiss")
    public ApiResponse<Void> dismiss(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                     @PathVariable String alertId) {
        alertService.dismiss(userId, alertId);
        return ApiResponse.message("처리되었습니다.");
    }
}
