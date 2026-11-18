package com.ganzithon.pulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ganzithon.pulse.dto.picture.PictureResponse;
import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.global.util.PublicUrlResolver;
import com.ganzithon.pulse.repository.PictureCatalog;
import com.ganzithon.pulse.repository.PictureCatalog.Picture;
import com.ganzithon.pulse.repository.PictureCatalog.Target;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Spring 없이 서비스 규칙만 확인한다 */
class PictureServiceTest {
    private static Picture picture(String id, Mode mode, String image) {
        return new Picture(id, mode, image, 0.5, List.of(new Target("t1", "고양이", "🐱", 0.5, 0.5, 0.05)));
    }

    private static PictureService service(String baseUrl, Picture... pictures) {
        return new PictureService(new PictureCatalog(List.of(pictures)), new PublicUrlResolver(baseUrl));
    }

    @Test
    void noPictureForModeIsPicture001() {
        PictureService service = service("https://api.example.com", picture("p1", Mode.OBJECT, "a.png"));
        BusinessException e = assertThrows(BusinessException.class, () -> service.next(Mode.COLOR));
        assertEquals(ErrorCode.PICTURE_001, e.errorCode());
    }

    @Test
    void onlyRequestedModeIsReturned() {
        PictureService service = service("https://api.example.com",
                picture("o1", Mode.OBJECT, "o1.png"), picture("c1", Mode.COLOR, "c1.png"),
                picture("c2", Mode.COLOR, "c2.png"));
        for (int i = 0; i < 50; i++) {
            assertEquals(Mode.COLOR, service.next(Mode.COLOR).mode());
        }
    }

    @Test
    void imageUrlUsesConfiguredBaseUrl() {
        PictureResponse r = service("https://api.example.com/", picture("p1", Mode.OBJECT, "park_01.png")).next(Mode.OBJECT);
        assertEquals("https://api.example.com/pictures/park_01.png", r.imageUrl());
    }
}
