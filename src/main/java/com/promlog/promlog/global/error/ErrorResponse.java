package com.promlog.promlog.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Object details,
        String traceId,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode, String message, Object details, String traceId) {
        return new ErrorResponse(
                errorCode.getCode(),
                message,
                details,
                traceId,
                OffsetDateTime.now()
        );
    }

    public static ErrorResponse of(String code, String message, Object details, String traceId) {
        return new ErrorResponse(
                code,
                message,
                details,
                traceId,
                OffsetDateTime.now()
        );
    }
}