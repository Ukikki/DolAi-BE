package com.dolai.backend.friend.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.friend.model.Friends;
import com.dolai.backend.friend.model.request.FriendRequestCreateDto;
import com.dolai.backend.friend.model.request.FriendActionRequestDto;
import com.dolai.backend.friend.model.response.FriendInfoDto;
import com.dolai.backend.friend.model.response.ReceivedFriendRequestDto;
import com.dolai.backend.friend.service.FriendsService;
import com.dolai.backend.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendsController {
    private final FriendsService friendsService;

    // 친구 목록 조회
    @GetMapping
    public ResponseEntity<?> getFriends(@AuthenticationPrincipal User user) {
        List<FriendInfoDto> friends = friendsService.getFriends(user.getId());
        return ResponseEntity.ok(new SuccessDataResponse<>(Map.of("friends", friends)));
    }

    // 친구 요청
    @PostMapping("/request")
    public ResponseEntity<?> requestFriend(@RequestBody @Valid FriendRequestCreateDto request,
                                           @AuthenticationPrincipal User user) {
        Friends friends = friendsService.requestFriend(user.getId(), request.getTargetUserId());
        return ResponseEntity.ok(
                new SuccessDataResponse<>(Map.of("requestedUser", ReceivedFriendRequestDto.from(friends))));
    }

    // 친구 수락 or 거절
    @PatchMapping("/respond/{requestId}")
    public ResponseEntity<?> respondToFriendRequest(
            @PathVariable("requestId") String requestIdStr,
            @RequestBody @Valid FriendActionRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        try {
            Long requestId = Long.parseLong(requestIdStr);

            switch (request.getAction().toLowerCase()) {
                case "accept" -> friendsService.acceptFriend(requestId, user.getId());
                case "reject" -> friendsService.rejectFriend(requestId, user.getId());
                default -> throw new IllegalArgumentException("action은 'accept' 또는 'reject'여야 합니다.");
            }

            return ResponseEntity.ok(new SuccessMessageResponse("Friend request " + request.getAction() + "ed successfully"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    new SuccessMessageResponse("잘못된 요청 ID 형식입니다.")
            );
        }
    }

    // 친구 삭제
    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> deleteFriend(@PathVariable("friendId") String friendId,
                                          @AuthenticationPrincipal User user) {

        friendsService.deleteFriend(user.getId(), friendId);
        return ResponseEntity.ok(new SuccessMessageResponse("Friend deleted successfully"));
    }

    // 받은 친구 요청 목록 (requestId 포함)
    @GetMapping("/requests")
    public ResponseEntity<?> getReceivedRequests(@AuthenticationPrincipal User user) {
        List<Friends> receivedRequests = friendsService.getReceivedFriendRequests(user.getId());

        List<ReceivedFriendRequestDto> requestDtos = receivedRequests.stream()
                .map(ReceivedFriendRequestDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                new SuccessDataResponse<>(Map.of("requests", requestDtos))
        );
    }
}
