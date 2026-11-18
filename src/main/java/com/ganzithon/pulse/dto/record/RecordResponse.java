package com.ganzithon.pulse.dto.record;

import com.ganzithon.pulse.entity.ActivityRecord;
import com.ganzithon.pulse.global.util.TimeFormat;

public record RecordResponse(Long recordId, String activityName, String pictureId,
                             Integer beforeScore, int afterScore, String createdAt) {
    public static RecordResponse from(ActivityRecord r) {
        return new RecordResponse(r.getId(), r.getActivityName(), r.getPictureId(),
                r.getBeforeScore(), r.getAfterScore(), TimeFormat.iso(r.getCreatedAt()));
    }
}
