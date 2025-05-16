package com.dolai.backend.screenshare.model;

import com.dolai.backend.screenshare.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScreenShareSocketEvent {
    private EventType type;
    private String userId;
}

