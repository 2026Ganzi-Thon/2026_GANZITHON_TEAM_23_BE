package com.ganzithon.pulse.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    COMMON_001("COMMON-001", HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    COMMON_002("COMMON-002", HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요."),
    AUTH_001("AUTH-001", HttpStatus.UNAUTHORIZED, "입력 내용을 확인해 주세요."),
    AUTH_002("AUTH-002", HttpStatus.CONFLICT, "이미 사용 중인 아이디예요."),
    AUTH_003("AUTH-003", HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),
    ALERT_001("ALERT-001", HttpStatus.NOT_FOUND, "알림을 찾을 수 없어요."),
    PICTURE_001("PICTURE-001", HttpStatus.NOT_FOUND, "그림을 불러오지 못했어요."),
    RECORD_001("RECORD-001", HttpStatus.NOT_FOUND, "기록을 찾을 수 없어요.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
    public String message() { return message; }
}
