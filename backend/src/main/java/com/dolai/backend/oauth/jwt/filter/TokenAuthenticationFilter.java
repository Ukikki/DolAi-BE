package com.dolai.backend.oauth.jwt.filter;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import com.dolai.backend.oauth.jwt.TokenKey;
import com.dolai.backend.oauth.jwt.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveToken(request);

        // 1. AccessToken이 있고, 유효하면 인증 처리
        if (StringUtils.hasText(accessToken) && tokenProvider.validateToken(accessToken)) {

            // 1-1. 블랙리스트 검사 (로그아웃된 토큰인지 확인)
            if (redisTemplate.opsForValue().get("blacklist:" + accessToken) != null) {
                log.warn("❌ 로그아웃된 토큰으로 요청이 들어옴: {}", accessToken);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }
            // 1-2. 인증 객체 설정
            setAuthentication(accessToken);
        }

        // 2. 그 외 (토큰이 없거나 만료됨): 인증 없이 통과 (익명 사용자로 처리)
        filterChain.doFilter(request, response);
    }


    private void setAuthentication(String accessToken) {
        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("🔍 TokenAuthenticationFilter Principal Type: {}", authentication.getPrincipal().getClass().getName());
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION);
        if (ObjectUtils.isEmpty(token) || !token.startsWith(TokenKey.TOKEN_PREFIX)) {
            return null;
        }
        return token.substring(TokenKey.TOKEN_PREFIX.length());
    }
}
