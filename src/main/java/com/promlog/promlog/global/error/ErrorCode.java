package com.promlog.promlog.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR"),

    // 401
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED"),

    // 403 (세분화)
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED"),
    REJOIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "REJOIN_NOT_ALLOWED"),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "ACCOUNT_DELETED"),
    TOKEN_EXPIRED(HttpStatus.FORBIDDEN, "TOKEN_EXPIRED"),
    TOKEN_INVALID(HttpStatus.FORBIDDEN, "TOKEN_INVALID"),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND"),

    // 409
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT"),
    OAUTH_ALREADY_LINKED(HttpStatus.CONFLICT, "OAUTH_ALREADY_LINKED"),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");

    private final HttpStatus status;
    private final String code;

    ErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
