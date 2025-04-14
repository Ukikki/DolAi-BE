package com.dolai.backend.notification.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.notification.model.NotificationResponseDto;
import com.dolai.backend.notification.service.NotificationService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal User user) {
        List<NotificationResponseDto> response = notificationService.getMyNotifications(user.getId());
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }
}
