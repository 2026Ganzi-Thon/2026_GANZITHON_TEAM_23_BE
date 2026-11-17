package com.ganzithon.pulse.global.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFormat {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeFormat() {}

    /** 2026-11-15T14:20:00+09:00 형태 */
    public static String iso(Instant instant) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(KST));
    }

    public static String yyyymmdd(Instant instant) {
        return DateTimeFormatter.BASIC_ISO_DATE.format(instant.atZone(KST));
    }
}
