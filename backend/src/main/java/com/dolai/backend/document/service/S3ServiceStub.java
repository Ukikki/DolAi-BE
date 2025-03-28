package com.dolai.backend.document.service;

import org.springframework.stereotype.Component;

@Component
public class S3ServiceStub {

    // 상세 문서 내용 가져오기
    public String fetchDetailedContent(String fileUrl) {
        return "회의에서 신규 프로젝트 일정 조율 및 주요 역할 분배가 논의되었습니다.";
    }

    // 그래프 이미지를 Base64 문자열로 가져오기
    public String fetchGraphImageBase64(String imageUrl) {
        return "data:image/png;base64,iVBORw0KGgoAAAANS"; // base64 대체 텍스트
    }

    // 회의 메모
    public String fetchNotes(String notesUrl) {
        return "회의 내용은 팀 내부 공유 문서에도 추가됨.";
    }
}
