// 발생하는 모든 예외를 처리하는 역할
package com.dolai.backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //CustomException 예외 처리 (dolAi에서 직접 정의한 예외들)
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e){
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    // 유효성 검사 실패(Validation Error) 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("⚠️ 유효성 검사 실패: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    // 잘못된 요청 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("🚨 IllegalArgumentException 발생: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_REQUEST, e.getMessage());
    }

    // 예상하지 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("🔥 예상하지 못한 예외 발생: {}", e.getMessage(), e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, "예상하지 못한 오류가 발생했습니다.");
    }

    // 공통 에러 응답을 생성하는 메서드
    private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse("error", message));
    }
}