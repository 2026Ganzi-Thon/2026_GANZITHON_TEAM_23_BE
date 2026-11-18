package com.ganzithon.pulse.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.global.exception.GlobalExceptionHandler;
import com.ganzithon.pulse.global.util.PublicUrlResolver;
import com.ganzithon.pulse.repository.PictureCatalog;
import com.ganzithon.pulse.repository.PictureCatalog.Picture;
import com.ganzithon.pulse.repository.PictureCatalog.Target;
import com.ganzithon.pulse.service.PictureService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 실제 컨트롤러·서비스·예외 처리기를 카탈로그만 바꿔 Spring 컨텍스트 없이 호출한다.
 * PulseFlowTest의 컨텍스트와 H2 DB를 건드리지 않고, 카탈로그에 색 모드 그림이 들어와도 깨지지 않는다.
 */
class PictureControllerTest {
    private static MockMvc mvc(Picture... pictures) {
        PictureService service = new PictureService(new PictureCatalog(List.of(pictures)),
                new PublicUrlResolver("https://api.example.com"));
        return MockMvcBuilders.standaloneSetup(new PictureController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static Picture objectPicture() {
        return new Picture("p1", Mode.OBJECT, "a.png", 0.5, List.of(new Target("t1", "고양이", "🐱", 0.5, 0.5, 0.05)));
    }

    @Test
    void emptyCatalogReturns404Picture001() throws Exception {
        for (Mode mode : Mode.values()) {
            mvc().perform(get("/api/v1/pictures/next").param("mode", mode.name()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("PICTURE-001"))
                    .andExpect(jsonPath("$.message").value("그림을 불러오지 못했어요."))
                    .andExpect(jsonPath("$.errors").isEmpty())
                    .andExpect(jsonPath("$.path").value("/api/v1/pictures/next"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Test
    void modeWithoutPictureReturns404WhileOtherModeIs200() throws Exception {
        MockMvc mvc = mvc(objectPicture());
        mvc.perform(get("/api/v1/pictures/next").param("mode", "COLOR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PICTURE-001"));
        mvc.perform(get("/api/v1/pictures/next").param("mode", "OBJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pictureId").value("p1"));
    }
}
