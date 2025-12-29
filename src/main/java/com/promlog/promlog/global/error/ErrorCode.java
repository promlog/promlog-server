package com.promlog.promlog.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    // 401
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND),

    // 409
    CONFLICT(HttpStatus.CONFLICT),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
