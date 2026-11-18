package com.ganzithon.pulse.dto.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordRequest(
        String pictureId,
        String alertId,
        @Min(value = 1, message = "점수는 1~5예요.") @Max(value = 5, message = "점수는 1~5예요.") Integer beforeScore,
        @NotNull(message = "활동 후 점수가 필요해요.")
        @Min(value = 1, message = "점수는 1~5예요.") @Max(value = 5, message = "점수는 1~5예요.") Integer afterScore) {}
