package com.ganzithon.pulse.dto.picture;

import com.ganzithon.pulse.repository.PictureCatalog;

public record TargetResponse(String targetId, String label, String emoji, double x, double y, double radius) {
    public static TargetResponse from(PictureCatalog.Target t) {
        return new TargetResponse(t.targetId(), t.label(), t.emoji(), t.x(), t.y(), t.radius());
    }
}
