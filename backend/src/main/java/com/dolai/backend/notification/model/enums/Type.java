package com.dolai.backend.notification.model.enums;

import lombok.Getter;
import java.util.Map;

@Getter
public enum Type {

    // 친구
    FRIEND_REQUEST("친구","‘{{sender}}’님에게 친구 요청을 받았습니다."),
    FRIEND_ACCEPT("친구","‘{{sender}}’님이 친구 요청을 수락했습니다."),
    FRIEND_REJECT("친구","‘{{sender}}’님이 친구 요청을 거절했습니다."),

    // 회의
    MEETING_INVITE("회의","‘{{meetingTitle}}’ 회의에 초대되었습니다. (호스트: ‘{{host}}’)"),
    MEETING_RESERVED("회의","‘{{meetingTitle}}’ 회의 일정이 ‘{{date}}’에 예약되었습니다. (호스트: ‘{{host}}’)"),
    MEETING_SOON("회의","‘{{time}}’ 뒤 ‘{{meetingTitle}}’ 회의가 시작됩니다."),
    MEETING_CANCELLED("회의", "‘{{meetingTitle}}’ 회의가 취소되었습니다. (호스트: ‘{{host}}’)"),

    // 일정
    TODO_CREATED("일정","{{assignee}}님에게 ‘{{todo}}’ To Do 일정이 추가되었습니다.");

    private final String category;
    private final String template;

    Type(String category, String template) {
        this.category = category;
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
