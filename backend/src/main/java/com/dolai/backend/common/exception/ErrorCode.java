package com.dolai.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request parameters"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // Auth
    OAUTH_PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OAuth provider not found"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 AccessToken을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EXPIRED_JWT(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT입니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT 서명이 유효하지 않습니다."),
    INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),

    // Directory
    DIRECTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "디렉터리를 찾을 수 없습니다."),
    MEETING_ID_REQUIRED(HttpStatus.BAD_REQUEST, "공유 디렉터리는 meetingId가 필요합니다."),
    MEETING_ID_SHOULD_BE_NULL(HttpStatus.BAD_REQUEST, "개인 디렉터리에는 meetingId를 포함할 수 없습니다."),

    // Document
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의에 연결된 문서를 찾을 수 없습니다."),

    // Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
