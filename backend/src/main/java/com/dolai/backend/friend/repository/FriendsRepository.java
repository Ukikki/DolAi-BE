package com.dolai.backend.friend.repository;

import com.dolai.backend.friend.model.Friends;
import com.dolai.backend.friend.model.enums.FriendsStatus;
import com.dolai.backend.friend.model.response.FriendInfoDto;
import com.dolai.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendsRepository extends JpaRepository<Friends, Long> {
    Optional<Friends> findByRequesterIdAndReceiverId(String requesterId, String receiverId);
    List<Friends> findAllByStatusAndRequesterIdOrStatusAndReceiverId(FriendsStatus status1, String requesterId, FriendsStatus status2, String receiverId);
    List<Friends> findAllByRequesterIdAndStatus(String requesterId, FriendsStatus status);
    List<Friends> findAllByReceiverIdAndStatus(String receiverId, FriendsStatus status);
    @Query("""
    SELECT new com.dolai.backend.friend.model.response.FriendInfoDto(
        CASE WHEN f.requester.id = :userId THEN f.receiver.id ELSE f.requester.id END,
        CASE WHEN f.requester.id = :userId THEN f.receiver.email ELSE f.requester.email END,
        CASE WHEN f.requester.id = :userId THEN f.receiver.name ELSE f.requester.name END
    )
    FROM Friends f
    WHERE (f.requester.id = :userId OR f.receiver.id = :userId)
      AND f.status = 'ACCEPTED'
      AND (
            LOWER(f.requester.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(f.requester.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(f.receiver.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(f.receiver.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    List<FriendInfoDto> searchFriendsByKeyword(@Param("userId") String userId, @Param("keyword") String keyword);
}