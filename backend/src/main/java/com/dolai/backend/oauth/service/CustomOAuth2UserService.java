// OAuth2 로그인 유저 정보 조회 및 저장을 담당하는 서비스 클래스
package com.dolai.backend.oauth.service;

import com.dolai.backend.oauth.model.OAuth2UserInfo;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2UserInfo 객체 생성 (OAuth 제공자별로 다르게 처리)
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of(registrationId, attributes);

        // DB에서 사용자 조회 또는 저장
        User user = getOrSave(oAuth2UserInfo);

        // attributes에 "sub" 추가 (구글 & 카카오 모두 처리)
        Map<String, Object> updatedAttributes = new HashMap<>(attributes);
        if ("kakao".equals(registrationId)) {
            updatedAttributes.put("sub", oAuth2UserInfo.getSub());  // Kakao의 "id"를 "sub"으로 저장
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
                updatedAttributes,
                userNameAttributeName // OAuth 제공자가 제공하는 기본 키 사용
        );
    }

    private User getOrSave(OAuth2UserInfo oAuth2UserInfo) {
        return userRepository.findByEmail(oAuth2UserInfo.getEmail())
                .map(existingUser -> {
                    // 기존 사용자 정보 업데이트 (변경이 있을 때만)
                    if (shouldUpdateProfileImage(existingUser, oAuth2UserInfo)) {
                        existingUser.setProfileImageUrl(oAuth2UserInfo.getProfileImageUrl());
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(oAuth2UserInfo.toEntity()));
    }

    private boolean shouldUpdateProfileImage(User user, OAuth2UserInfo userInfo) {
        String currentImage = user.getProfileImageUrl();
        String newImage = userInfo.getProfileImageUrl();

        return currentImage == null ||
                currentImage.contains("kakao") ||
                currentImage.contains("google") ||
                currentImage.startsWith("http") &&
                        !currentImage.equals(newImage);
    }

}
