package com.ganzithon.pulse.global.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public final class TimeFormat {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TimeFormat() {}

    /** 2026-11-15T14:20:00+09:00 형태 */
    public static String iso(Instant instant) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.truncatedTo(ChronoUnit.SECONDS).atZone(KST));
    }

    /** 20261115 형태 (알림 ID용) */
    public static String yyyymmdd(Instant instant) {
        return YYYYMMDD.format(instant.atZone(KST));
    }
}
