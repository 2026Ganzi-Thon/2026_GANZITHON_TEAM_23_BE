package com.ganzithon.pulse.controller;

import com.ganzithon.pulse.dto.picture.PictureResponse;
import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.global.response.ApiResponse;
import com.ganzithon.pulse.service.PictureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pictures")
public class PictureController {
    private final PictureService pictureService;

    public PictureController(PictureService pictureService) {
        this.pictureService = pictureService;
    }

    /** alertId는 계약을 드러내려고 받기만 하고, 존재 여부를 확인하지 않으며 그림 선택에도 쓰지 않는다 */
    @GetMapping("/next")
    public ApiResponse<PictureResponse> next(@RequestParam Mode mode,
                                             @RequestParam(required = false) String alertId) {
        return ApiResponse.ok(pictureService.next(mode));
    }
}
