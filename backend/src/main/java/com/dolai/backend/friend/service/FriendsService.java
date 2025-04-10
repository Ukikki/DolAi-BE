package com.dolai.backend.friend.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.friend.model.Friends;
import com.dolai.backend.friend.model.enums.FriendsStatus;
import com.dolai.backend.friend.model.response.FriendInfoDto;
import com.dolai.backend.friend.repository.FriendsRepository;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendsService {
    private final FriendsRepository friendsRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public Friends requestFriend(String requesterId, String receiverId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECEIVER_NOT_FOUND));

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
        return friendsRepository.save(friends);
    }

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

    // ⭐ 받은 친구 요청 목록 (Entity 그대로 반환)
    @Transactional(readOnly = true)
    public List<Friends> getReceivedFriendRequests(String userId) {
        return friendsRepository.findAllByReceiverIdAndStatus(userId, FriendsStatus.REQUESTED);
    }
}
