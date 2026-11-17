package com.ganzithon.pulse.global.exception;

import com.ganzithon.pulse.global.util.TimeFormat;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(String timestamp, int status, String code, String message,
                            List<FieldDetail> errors, String path) {

    public record FieldDetail(String field, Object value, String reason) {}

    public static ErrorResponse of(int status, String code, String message,
                                   List<FieldDetail> errors, String path) {
        return new ErrorResponse(TimeFormat.iso(Instant.now()), status, code, message, errors, path);
    }
}
