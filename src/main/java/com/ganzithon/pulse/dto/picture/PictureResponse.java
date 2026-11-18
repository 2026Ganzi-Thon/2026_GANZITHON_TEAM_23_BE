package com.ganzithon.pulse.dto.picture;

import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.repository.PictureCatalog;
import java.util.List;

public record PictureResponse(String pictureId, Mode mode, String imageUrl, double aspectRatio,
                              List<TargetResponse> targets) {
    public static PictureResponse from(PictureCatalog.Picture p, String imageUrl) {
        return new PictureResponse(p.pictureId(), p.mode(), imageUrl, p.aspectRatio(),
                p.targets().stream().map(TargetResponse::from).toList());
    }
}
