package com.ganzithon.pulse.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException e, HttpServletRequest req) {
        ErrorCode ec = e.errorCode();
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.status().value(), ec.code(), ec.message(), List.of(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErrorResponse.FieldDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldDetail(f.getField(),
                        "password".equals(f.getField()) ? null : f.getRejectedValue(),
                        f.getDefaultMessage()))
                .toList();
        return bad(errors, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        return bad(List.of(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return bad(List.of(new ErrorResponse.FieldDetail(e.getName(), e.getValue(), "값의 형식이 올바르지 않아요.")), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unknown(Exception e, HttpServletRequest req) {
        // 405, 404 등 Spring이 만드는 표준 오류는 상태 코드를 그대로 살린다
        if (e instanceof org.springframework.web.ErrorResponse spring) {
            int status = spring.getStatusCode().value();
            return ResponseEntity.status(status)
                    .body(ErrorResponse.of(status, ErrorCode.COMMON_001.code(), ErrorCode.COMMON_001.message(),
                            List.of(), req.getRequestURI()));
        }
        log.error("unhandled", e);
        ErrorCode ec = ErrorCode.COMMON_002;
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.status().value(), ec.code(), ec.message(), List.of(), req.getRequestURI()));
    }

    private ResponseEntity<ErrorResponse> bad(List<ErrorResponse.FieldDetail> errors, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.COMMON_001;
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.status().value(), ec.code(), ec.message(), errors, req.getRequestURI()));
    }
}
