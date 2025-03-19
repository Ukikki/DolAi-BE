// OAuth2 로그인 유저 정보 조회 및 저장을 담당하는 서비스 클래스
package com.dolai.backend.oauth.service;
import com.dolai.backend.oauth.info.OAuth2UserInfo;
import com.dolai.backend.user.domain.User;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // OAuth2 로그인 유저 정보를 가져옴
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("[OAuth2 원본 데이터] getAttributes : {}", oAuth2User.getAttributes());

        // provider : kakao, naver
        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("[OAuth Provider] provider : {}", provider);

        // 필요한 정보를 provider에 따라 다르게 mapping
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoService.getOAuth2UserInfo(provider, oAuth2User.getAttributes());
        log.info("[OAuth2UserInfo] Email: {}", oAuth2UserInfo.getEmail());
        log.info("[OAuth2UserInfo] Name: {}", oAuth2UserInfo.getName());
        log.info("[OAuth2UserInfo] Profile Image: {}", oAuth2UserInfo.getProfileImageUrl());

        // email이 null이면 에러 발생
        if (oAuth2UserInfo.getEmail() == null) {
            throw new IllegalArgumentException("OAuth2 로그인에서 email을 가져올 수 없습니다.");
        }

        // 유저 정보 확인 (email 기반으로 조회)
        User user = userRepository.findByEmail(oAuth2UserInfo.getEmail())
                .map(entity -> entity.update(oAuth2UserInfo.getName(), oAuth2UserInfo.getProfileImageUrl()))
                .orElseGet(() -> userRepository.save(oAuth2UserInfo.toEntity()));
        log.info("[DB 저장된 유저 정보] user : {}", user.toString());

        // UserDetails와 OAuth2User를 다중 상속한 CustomUserDetails
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                oAuth2User.getAttributes(),
                "email"
        );    }
}