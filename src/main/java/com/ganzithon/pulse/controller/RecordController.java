package com.ganzithon.pulse.controller;

import com.ganzithon.pulse.dto.record.NextActionRequest;
import com.ganzithon.pulse.dto.record.RecordRequest;
import com.ganzithon.pulse.dto.record.RecordResponse;
import com.ganzithon.pulse.global.auth.AuthInterceptor;
import com.ganzithon.pulse.global.response.ApiResponse;
import com.ganzithon.pulse.service.RecordService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/records")
public class RecordController {
    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ApiResponse<List<RecordResponse>> list(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return ApiResponse.ok(recordService.list(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecordResponse> save(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                            @Valid @RequestBody RecordRequest request) {
        return ApiResponse.ok(recordService.save(userId, request));
    }

    @PostMapping("/{recordId}/next-action")
    public ApiResponse<Void> recordNextAction(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                              @PathVariable Long recordId,
                                              @Valid @RequestBody NextActionRequest request) {
        recordService.recordNextAction(userId, recordId, request);
        return ApiResponse.message("처리되었습니다.");
    }
}
