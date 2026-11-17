package com.ganzithon.pulse.record;

import com.ganzithon.pulse.global.util.TimeFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class RecordDtos {
    private RecordDtos() {}

    public record RecordRequest(
            String pictureId,
            String alertId,
            @Min(value = 1, message = "점수는 1~5예요.") @Max(value = 5, message = "점수는 1~5예요.") Integer beforeScore,
            @NotNull(message = "활동 후 점수가 필요해요.")
            @Min(value = 1, message = "점수는 1~5예요.") @Max(value = 5, message = "점수는 1~5예요.") Integer afterScore) {}

    public record RecordResponse(Long recordId, String activityName, String pictureId,
                                 Integer beforeScore, int afterScore, String createdAt) {
        static RecordResponse from(ActivityRecord r) {
            return new RecordResponse(r.getId(), r.getActivityName(), r.getPictureId(),
                    r.getBeforeScore(), r.getAfterScore(), TimeFormat.iso(r.getCreatedAt()));
        }
    }
}
