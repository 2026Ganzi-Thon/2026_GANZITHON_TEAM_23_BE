package com.ganzithon.pulse.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 활동 모드. COLOR는 색 모드, OBJECT는 사물 모드 */
public enum Mode {
    COLOR, OBJECT;

    /** JSON 본문은 이름 문자열만 받는다. 0, "1" 같은 순번은 읽기 오류(400)가 된다 */
    @JsonCreator
    public static Mode from(String value) {
        for (Mode mode : values()) {
            if (mode.name().equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("정의되지 않은 모드입니다: " + value);
    }
}
