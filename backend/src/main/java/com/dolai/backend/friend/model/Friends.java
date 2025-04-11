package com.dolai.backend.friend.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.friend.model.enums.FriendsStatus;
import com.dolai.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "friends")
public class Friends extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 친구 신청 보낸 사람
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    // 친구 신청 받은 사람
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendsStatus status;

    public static Friends create(User requester, User receiver) {
        return Friends.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendsStatus.REQUESTED)
                .build();
    }
}
