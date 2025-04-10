package com.dolai.backend.notification.model.enums;

import java.util.Map;

public enum Type {

    // 알림 종류 + 템플릿 ({{sender}}는 나중에 대체됨)
    FRIEND_REQUEST("{{sender}}님이 친구 요청을 보냈습니다."),
    FRIEND_ACCEPT("{{sender}}님이 친구 요청을 수락했습니다."),
    FRIEND_REJECT("{{sender}}님이 친구 요청을 거절했습니다."),
    MEETING_INVITE("{{sender}}님이 회의에 초대했습니다."),
    SCHEDULE_CREATED("회의 일정이 생성되었습니다.");

    private final String template;

    Type(String template) {
        this.template = template;
    }

    public String format(Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
