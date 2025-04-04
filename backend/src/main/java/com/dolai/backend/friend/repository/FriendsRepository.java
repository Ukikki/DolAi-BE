package com.dolai.backend.friend.repository;

import com.dolai.backend.friend.model.Friends;
import com.dolai.backend.friend.model.enums.FriendsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendsRepository extends JpaRepository<Friends, Long> {
    Optional<Friends> findByRequesterIdAndReceiverId(String requesterId, String receiverId);
    List<Friends> findAllByRequesterIdOrReceiverIdAndStatus(String userId1, String userId2, FriendsStatus status);
    List<Friends> findAllByReceiverIdAndStatus(String receiverId, FriendsStatus status);
}
