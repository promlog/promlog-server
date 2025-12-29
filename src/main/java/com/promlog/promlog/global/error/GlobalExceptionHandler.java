package com.promlog.promlog.global.error;

import com.promlog.promlog.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.promlog.promlog")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", e.getErrorCode().name(),
                                "message", e.getMessage(),
                                "details", e.getDetails()
                        )
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity
                .status(500)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "INTERNAL_ERROR",
                                "message", "서버 오류가 발생했습니다."
                        )
                ));
    }
}
