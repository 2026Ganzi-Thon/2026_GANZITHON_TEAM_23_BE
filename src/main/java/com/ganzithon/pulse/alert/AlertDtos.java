package com.ganzithon.pulse.alert;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public final class AlertDtos {
    private AlertDtos() {}

    public record SpikeRequest(
            @NotNull(message = "bpm이 필요해요.") @Min(value = 1, message = "bpm은 1 이상이에요.") Integer bpm,
            @NotNull(message = "baselineBpm이 필요해요.") @Min(value = 1, message = "baselineBpm은 1 이상이에요.") Integer baselineBpm,
            Integer steps,
            @NotNull(message = "measuredAt이 필요해요.") OffsetDateTime measuredAt) {}

    public record SpikeResponse(boolean shouldAlert, String alertId) {}
}
