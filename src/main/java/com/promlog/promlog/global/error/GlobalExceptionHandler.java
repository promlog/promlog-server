package com.promlog.promlog.global.error;

import com.promlog.promlog.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice(basePackages = "com.promlog.promlog")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        String traceId = getOrCreateTraceId(request);

        // 비즈니스 예외는 warn 정도
        log.warn("[BusinessException] code={}, message={}, traceId={}",
                e.getErrorCode().getCode(), e.getMessage(), traceId);

        ErrorResponse error = ErrorResponse.of(
                e.getErrorCode(),
                e.getMessage(),
                normalizeDetails(e.getDetails()),
                traceId
        );

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(error));
    }

    /**
     * ✅ @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String traceId = getOrCreateTraceId(request);

        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            // 같은 필드가 여러 번 걸릴 수 있어도 마지막 값으로 덮어씀(원하면 List로 바꿀 수 있음)
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponse error = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                "요청 값이 올바르지 않습니다.",
                Map.of("fields", fieldErrors),
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiResponse.fail(error));
    }

    /**
     * ✅ 모든 예외 fallback (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        String traceId = getOrCreateTraceId(request);

        // 500은 반드시 stacktrace 로그
        log.error("[UnhandledException] traceId={}", traceId, e);

        ErrorResponse error = ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR,
                "서버 오류가 발생했습니다.",
                null,
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(error));
    }

    private Object normalizeDetails(Object details) {
        // null이면 아예 안 보내고 싶으면 ErrorResponse에서 @JsonInclude 쓰는 방식도 가능
        // 지금은 null 그대로 둬도 ApiResponse에 NON_NULL이 걸려있지 않으니 details는 null로 나갈 수 있음.
        return details;
    }

    /**
     * traceId를 헤더에서 받거나(게이트웨이/NGINX/ELB), 없으면 생성
     */
    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }
}