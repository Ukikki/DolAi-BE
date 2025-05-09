package com.dolai.backend.user.service;

import com.dolai.backend.oauth.model.OAuth2UserInfo;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import com.dolai.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            String updateProfileImage = existingUser.getProfileImageUrl();
            String socialProfileImage = userInfo.getProfileImageUrl();

            boolean isUsingSocialImage =
                    updateProfileImage == null ||
                            updateProfileImage.contains("kakao") ||
                            updateProfileImage.contains("google") ||
                            updateProfileImage.startsWith("http");

            String imageToUpdate = isUsingSocialImage ? socialProfileImage : updateProfileImage;

            existingUser.setProfileImageUrl(imageToUpdate);

            log.info("🔄 기존 사용자 프로필이미지만 업데이트: {}", existingUser);
            return userRepository.save(existingUser);
        } else {
            // 신규 유저 저장
            User newUser = User.create(
                    userInfo.getSub(),
                    userInfo.getEmail(),
                    userInfo.getName(),
                    userInfo.getProfileImageUrl(),
                    userInfo.getProvider()
            );

            log.info("🆕 신규 사용자 저장: {}", newUser);
            return userRepository.save(newUser);
        }
    }

    public List<UserDto> findUserByEmail(String email) {
        List<User> users = userRepository.findByEmailContainingIgnoreCase(email);
        return users.stream().map(UserDto::create).collect(Collectors.toList());
    }}
