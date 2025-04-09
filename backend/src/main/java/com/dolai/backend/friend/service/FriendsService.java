package com.dolai.backend.friend.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.friend.model.Friends;
import com.dolai.backend.friend.model.response.FriendInfoDto;
import com.dolai.backend.friend.model.enums.FriendsStatus;
import com.dolai.backend.friend.repository.FriendsRepository;
import com.dolai.backend.notification.model.Notification;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.service.NotificationService;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendsService {
    private final FriendsRepository friendsRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 친구 목록 조회 ( friendsStatus가 ACCEPTED인 )
    @Transactional(readOnly = true)
    public List<FriendInfoDto> getFriends(String userId) {
        List<Friends> friendsList = friendsRepository
                .findAllByRequesterIdOrReceiverIdAndStatus(userId, userId, FriendsStatus.ACCEPTED);
        return friendsList.stream()
                .map(friends -> {
                    User friend = friends.getRequester().getId().equals(userId)
                            ? friends.getReceiver()
                            : friends.getRequester();
                    return FriendInfoDto.create(friend);
                })
                .collect(Collectors.toList());
    }

    // 친구 요청
    @Transactional
    public Friends requestFriend(String requesterId, String receiverId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECEIVER_NOT_FOUND));

        // 기존 친구 관계 확인 (양방향)
        Optional<Friends> existing = friendsRepository
                .findByRequesterIdAndReceiverId(requesterId, receiverId)
                .or(() -> friendsRepository.findByRequesterIdAndReceiverId(receiverId, requesterId));

        if (existing.isPresent()) {
            Friends friends = existing.get();
            switch (friends.getStatus()) {
                case REQUESTED -> throw new CustomException(ErrorCode.FRIEND_ALREADY_REQUESTED);
                case ACCEPTED -> throw new CustomException(ErrorCode.FRIEND_ALREADY_ACCEPTED);
                case REJECTED -> friendsRepository.delete(friends);
            }
        }

        Friends friends = Friends.create(requester, receiver);
        //return friendsRepository.save(friends);

        Friends saved = friendsRepository.save(friends);

        // 실시간 알림 전송 (템플릿 기반)
        notificationService.notify(
                receiverId,
                Type.FRIEND_REQUEST,
                Map.of("sender", requester.getName())
        );
        return saved;
    }


    // 친구 수락
    @Transactional
    public void acceptFriend(Long friendsId, String currentUserId) {
        Friends friends = friendsRepository.findById(friendsId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friends.getReceiver().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_NOT_YOURS);
        }

        friends.setStatus(FriendsStatus.ACCEPTED);
        friendsRepository.save(friends);
    }

    // 친구 거절
    @Transactional
    public void rejectFriend(Long friendsId, String currentUserId) {
        Friends friends = friendsRepository.findById(friendsId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friends.getReceiver().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_NOT_YOURS);
        }

        friends.setStatus(FriendsStatus.REJECTED);
        friendsRepository.save(friends);
    }

    // 친구 삭제
    @Transactional
    public void deleteFriend(String userId, String friendId) {
        Optional<Friends> friendsOpt = friendsRepository.findByRequesterIdAndReceiverId(userId, friendId);

        if (friendsOpt.isEmpty()) {
            friendsOpt = friendsRepository.findByRequesterIdAndReceiverId(friendId, userId);
        }

        Friends friends = friendsOpt
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));

        friendsRepository.delete(friends);
    }

    // 받은 친구 요청 목록
    @Transactional(readOnly = true)
    public List<FriendInfoDto> getReceivedFriendRequests(String userId) {
        List<Friends> requests = friendsRepository
                .findAllByReceiverIdAndStatus(userId, FriendsStatus.REQUESTED);

        return requests.stream()
                .map(friends -> FriendInfoDto.create(friends.getRequester()))
                .collect(Collectors.toList());
    }

    // 내가 보낸 요청 목록
    public List<FriendInfoDto> getSentFriendRequests(String userId) {
        List<Friends> requests = friendsRepository.findAllByRequesterIdAndStatus(userId, FriendsStatus.REQUESTED);
        return requests.stream()
                .map(f -> FriendInfoDto.create(f.getReceiver()))
                .toList();
    }

    // 내가 보낸 요청 취소
    public void cancelSentFriendRequest(String userId, String friendId) {
        Friends request = friendsRepository.findById(Long.parseLong(friendId))
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));
        if (!request.getStatus().equals(FriendsStatus.REQUESTED)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_STATUS);
        }
        friendsRepository.delete(request);
    }

    // 친구 검색
    public List<FriendInfoDto> searchFriendsByKeyword(String userId, String keyword) {
        String cleanKeyword = keyword.trim(); // 앞뒤 공백 제거
        return friendsRepository.searchFriendsByKeyword(userId, cleanKeyword);
    }
}
