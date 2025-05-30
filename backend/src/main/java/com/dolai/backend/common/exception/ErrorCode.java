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
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),

    // Auth
    OAUTH_PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OAuth provider not found"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 AccessToken을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
    EXPIRED_JWT(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT입니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT 서명이 유효하지 않습니다."),
    INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),

    // Directory
    DIRECTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "디렉터리를 찾을 수 없습니다."),
    MEETING_ID_REQUIRED(HttpStatus.BAD_REQUEST, "공유 디렉터리는 meetingId가 필요합니다."),
    MEETING_ID_SHOULD_BE_NULL(HttpStatus.BAD_REQUEST, "개인 디렉터리에는 meetingId를 포함할 수 없습니다."),
    DIRECTORY_USER_NOT_FOUND(HttpStatus.FORBIDDEN, "해당 디렉터리에 대한 접근 권한이 없습니다."),

    // Document
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의에 연결된 문서를 찾을 수 없습니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 파일입니다."),

    // Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다."),
    MEETING_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "호스트가 회의를 시작하지 않았습니다."),
    MEETING_ENDED(HttpStatus.BAD_REQUEST, "종료된 회의입니다."),
    MEETING_HOST_ONLY(HttpStatus.FORBIDDEN, "회의 주최자만 회의를 종료할 수 있습니다."),
    STT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의의 자막 로그를 찾을 수 없습니다."),

    //Friends
    FRIEND_ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 친구 요청이 존재합니다."),
    FRIEND_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "이미 친구 상태입니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 관계가 존재하지 않습니다."),
    FRIEND_REQUEST_NOT_YOURS(HttpStatus.FORBIDDEN, "본인에게 온 요청만 수락 또는 거절할 수 있습니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 요청이 존재하지 않습니다."),
    USER_RECEIVER_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 요청 받을 사용자를 찾을 수 없습니다."),
    INVALID_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "대기 상태인 친구 요청만 취소할 수 있습니다."),

    // Users
    USER_FILE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "파일 업로드를 실패하였습니다."),
    USER_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // To-do
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 할 일을 찾을 수 없습니다."),

    // Metadata
    METADATA_NOT_FOUND(HttpStatus.NOT_FOUND, "디렉터리 메타데이터를 찾을 수 없습니다."),
    METADATA_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 메타데이터에 접근할 권한이 없습니다."),

    // Admin
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");


    private final HttpStatus status;
    private final String message;
}
