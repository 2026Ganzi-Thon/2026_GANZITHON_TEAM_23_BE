package com.ganzithon.pulse.dto.record;

import com.ganzithon.pulse.entity.ActivityRecord;
import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.entity.NextAction;
import com.ganzithon.pulse.global.util.TimeFormat;

public record RecordResponse(Long recordId, String activityName, Mode mode, String pictureId,
                             Integer beforeScore, int afterScore, NextAction nextAction, String createdAt) {
    public static RecordResponse from(ActivityRecord r) {
        return new RecordResponse(r.getId(), r.getActivityName(), r.getMode(), r.getPictureId(),
                r.getBeforeScore(), r.getAfterScore(), r.getNextAction(), TimeFormat.iso(r.getCreatedAt()));
    }
}
