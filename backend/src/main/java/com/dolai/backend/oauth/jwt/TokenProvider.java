// JWT 생성, 검증, 파싱
package com.dolai.backend.oauth.jwt;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.service.TokenService;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Component
@Primary
public class TokenProvider {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30L;  // 30분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60L * 24 * 7;  // 7일
    private static final String KEY_ROLE = "role";

    @Value("${jwt.key}")
    private String key;
    private SecretKey secretKey;

    @PostConstruct
    private void setSecretKey() {
        log.info("🔑 JWT Secret Key 초기화 중...");

        if (key == null || key.isEmpty()) {
            log.error("❌ JWT 키가 설정되지 않았습니다.");
        } else {
            secretKey = Keys.hmacShaKeyFor(key.getBytes());
            log.info("✅ JWT Secret Key 설정 완료!");
        }
    }

    // AccessToken 생성
    public String generateAccessToken(Authentication authentication) {
        log.info("🔹 AccessToken 생성 요청 - 사용자: {}", authentication.getName());
        return generateToken(authentication, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // RefreshToken 생성 후 저장
    public String generateRefreshToken(Authentication authentication, String accessToken) {
        log.info("🔹 RefreshToken 생성 요청 - 사용자: {}", authentication.getName());
        User user = (User) authentication.getPrincipal();
        String refreshToken = generateToken(authentication, REFRESH_TOKEN_EXPIRE_TIME);
        tokenService.saveOrUpdate(user.getId(), refreshToken, accessToken);
        return refreshToken;
    }

    // JWT 토큰 생성
    private String generateToken(Authentication authentication, long expireTime) {
        log.info("🔍 JWT 생성 시작...");

        if (authentication == null) {
            log.error("❌ authentication 객체가 null입니다!");
            return null;
        }

        log.info("✅ 사용자: {}, 권한: {}", authentication.getName(), authentication.getAuthorities());

        Date now = new Date();
        Date expiredDate = new Date(now.getTime() + expireTime); // 만료 시간 계산

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(getId(authentication)) // sub(고유 ID)를 subject로 설정
                .claim(KEY_ROLE, authorities) // 권한 정보를 claims에 저장
                .issuedAt(now)  // 토큰 발급 시간
                .expiration(expiredDate)  // 토큰 만료 시간
                .signWith(secretKey, Jwts.SIG.HS512)  // HMAC SHA-512 알고리즘으로 서명
                .compact();  // JWT 문자열로 변환
    }

    // AccessToken 재발급 (RefreshToken 검증 후)
    public String reissueAccessToken(String refreshToken) {
        log.info("🔄 AccessToken 재발급 요청");

        if (StringUtils.hasText(refreshToken) && validateToken(refreshToken)) {
            String userId = parseClaims(refreshToken).getSubject();

            // ✅ Redis에서 저장된 리프레시 토큰이 맞는지 확인
            String storedRefreshToken = tokenService.findRefreshTokenByUserId(userId);
            if (!refreshToken.equals(storedRefreshToken)) {
                log.warn("❌ 저장된 RefreshToken과 일치하지 않음");
                throw new CustomException(ErrorCode.INVALID_JWT);
            }

            Authentication authentication = getAuthentication(refreshToken);
            String reissuedAccessToken = generateAccessToken(authentication);

            log.info("✅ AccessToken 재발급 완료: {}", reissuedAccessToken);
            return reissuedAccessToken;
        }

        log.warn("⚠️ AccessToken 재발급 실패");
        return null;
    }


    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        log.debug("🔎 토큰 유효성 검사 시작");

        if (!StringUtils.hasText(token)) {
            log.warn("⚠️ 검증 실패: 토큰이 비어 있음");
            return false;
        }
        Claims claims = parseClaims(token);
        boolean isValid = claims.getExpiration().after(new Date());

        log.info("✅ 토큰 검증 결과: {}", isValid ? "유효함" : "만료됨");
        return isValid;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ JWT 만료됨: {}", e.getClaims().getSubject());
            throw new CustomException(ErrorCode.EXPIRED_JWT);
        } catch (MalformedJwtException e) {
            log.error("❌ JWT 형식 오류: {}", token);
            throw new CustomException(ErrorCode.INVALID_JWT);
        } catch (SecurityException e) {
            log.error("❌ JWT 서명 오류: {}", token);
            throw new CustomException(ErrorCode.INVALID_SIGNATURE);
        }
    }


    // Authentication 객체에서 사용자 ID(sub) 가져오기
    private String getId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        log.error("❌ 사용자 인증 정보에서 ID를 찾을 수 없음");
        throw new CustomException(ErrorCode.INVALID_AUTHENTICATION);
    }

    // 토큰에서 사용자 정보 추출
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        List<SimpleGrantedAuthority> authorities = getAuthorities(claims);
        String sub = claims.getSubject();

        log.info("🔍 토큰에서 사용자 정보 추출 - ID: {}", sub);

        User user = userRepository.findById(sub)
                .orElseThrow(() -> {
                    log.error("❌ User not found with sub: {}", sub);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        return new UsernamePasswordAuthenticationToken(user, token, authorities);
    }

    // 권한 정보 추출
    private List<SimpleGrantedAuthority> getAuthorities(Claims claims) {
        return Collections.singletonList(new SimpleGrantedAuthority(claims.get(KEY_ROLE).toString()));
    }

    public long getExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis(); // 만료 시간 - 현재 시간
    }

}
