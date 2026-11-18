package com.ganzithon.pulse.service;

import com.ganzithon.pulse.dto.picture.PictureResponse;
import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.global.util.PublicUrlResolver;
import com.ganzithon.pulse.repository.PictureCatalog;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class PictureService {
    private final PictureCatalog catalog;
    private final PublicUrlResolver urlResolver;

    public PictureService(PictureCatalog catalog, PublicUrlResolver urlResolver) {
        this.catalog = catalog;
        this.urlResolver = urlResolver;
    }

    /** 요청한 모드의 그림 중 한 장을 무작위로 고른다. 사용자별 이력은 보지 않는다 */
    public PictureResponse next(Mode mode) {
        List<PictureCatalog.Picture> pictures = catalog.findByMode(mode);
        if (pictures.isEmpty()) {
            throw new BusinessException(ErrorCode.PICTURE_001);
        }
        PictureCatalog.Picture picked = pictures.get(ThreadLocalRandom.current().nextInt(pictures.size()));
        return PictureResponse.from(picked, urlResolver.pictureUrl(picked.image()));
    }
}
