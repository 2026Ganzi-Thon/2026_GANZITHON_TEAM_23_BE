package com.ganzithon.pulse.dto.record;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.global.util.StrictIntegerDeserializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordRequest(
        @NotNull(message = "모드가 필요해요.") Mode mode,
        @Size(max = 255, message = "그림 ID는 255자 이하예요.") String pictureId,
        @Size(max = 255, message = "알림 ID는 255자 이하예요.") String alertId,
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Min(value = 1, message = "점수는 1~5예요.") @Max(value = 5, message = "점수는 1~5예요.") Integer beforeScore,
        @NotNull(message = "활동 후 점수가 필요해요.") @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Min(value = 1, message = "점수는 1~5예요.") @Max(value = 5, message = "점수는 1~5예요.") Integer afterScore) {}
