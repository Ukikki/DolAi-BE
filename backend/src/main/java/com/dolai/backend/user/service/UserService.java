package com.dolai.backend.user.service;

import com.dolai.backend.oauth.model.OAuth2UserInfo;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User saveOrUpdate(OAuth2UserInfo userInfo) {
        Optional<User> optionalUser = userRepository.findById(userInfo.getSub());

        if (optionalUser.isPresent()) {
            // 기존 유저 정보 업데이트
            User existingUser = optionalUser.get();
            existingUser.update(userInfo.getName(), userInfo.getProfileImageUrl());
            log.info("🔄 기존 사용자 업데이트: {}", existingUser);
            return userRepository.save(existingUser);
        } else {
            // 신규 유저 저장
            User newUser = User.builder()
                    .id(userInfo.getSub()) // OAuth2 Provider의 고유 ID 사용
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .profileImageUrl(userInfo.getProfileImageUrl())
                    .provider(userInfo.getProvider()) // 이미 Provider Enum이므로 그대로 사용
                    .build();
            log.info("🆕 신규 사용자 저장: {}", newUser);
            return userRepository.save(newUser);
        }
    }

    public Optional<User> getUserById(String sub) {
        return userRepository.findById(sub);
    }
}
