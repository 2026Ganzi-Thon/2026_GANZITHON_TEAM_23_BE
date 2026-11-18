package com.ganzithon.pulse.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 활동 뒤에 사용자가 고른 다음 행동. IMPROVED는 나아졌어요, RETRY는 한 번 더, HELP는 도움 요청 */
public enum NextAction {
    IMPROVED, RETRY, HELP;

    /** JSON 본문은 이름 문자열만 받는다. 1, "2" 같은 순번은 읽기 오류(400)가 된다 */
    @JsonCreator
    public static NextAction from(String value) {
        for (NextAction action : values()) {
            if (action.name().equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("정의되지 않은 다음 행동입니다: " + value);
    }
}
