package com.dolai.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request parameters"),

    OAUTH_PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OAuth provider not found"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 AccessToken을 찾을 수 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    EXPIRED_JWT(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),

    INVALID_JWT(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT입니다."),

    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT 서명이 유효하지 않습니다."),

    INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");


    private final HttpStatus httpStatus;
    private final String message;
}
