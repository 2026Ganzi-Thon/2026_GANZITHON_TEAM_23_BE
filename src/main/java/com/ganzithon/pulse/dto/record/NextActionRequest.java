package com.ganzithon.pulse.dto.record;

import com.ganzithon.pulse.entity.NextAction;
import jakarta.validation.constraints.NotNull;

public record NextActionRequest(@NotNull(message = "다음 행동이 필요해요.") NextAction action) {}
