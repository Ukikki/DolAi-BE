package com.dolai.backend.user.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.model.OAuth2UserInfo;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
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
            String updateProfileImage = existingUser.getProfileImageUrl();
            String socialProfileImage = userInfo.getProfileImageUrl();

            boolean isUsingSocialImage =
                    updateProfileImage == null ||
                            updateProfileImage.contains("kakao") ||
                            updateProfileImage.contains("google") ||
                            updateProfileImage.startsWith("http");

            String updateName = existingUser.getName(); // 기존 이름 유지
            String imageToUpdate = isUsingSocialImage ? socialProfileImage : updateProfileImage;

            existingUser.update(updateName, imageToUpdate);

            log.info("🔄 기존 사용자 프로필이미지만 업데이트: {}", existingUser);
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

    public UserDto findUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        UserDto userDto = UserDto.create(user);
        return userDto;
    }}
