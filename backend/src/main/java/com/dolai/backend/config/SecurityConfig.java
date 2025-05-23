// Spring Security를 활용한 OAuth2 로그인 및 보안 설정을 정의하는 클래스
package com.dolai.backend.config;

import com.dolai.backend.oauth.jwt.filter.TokenAuthenticationFilter;
import com.dolai.backend.oauth.jwt.filter.TokenExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 인증 로그인 비활성화
                .csrf(AbstractHttpConfigurer::disable) // csrf 비활성화
                .cors(c -> c.configurationSource(corsConfigurationSource())) // 명시적 적용
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용하지 않음
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/reissue").permitAll() // reissue(403) 예외 처리
                        .requestMatchers("/auth/social", "/auth/social/**", "/auth/callback").permitAll()  // 👈 소셜 로그인 요청은 인증 없이 허용
                        .requestMatchers("/auth/**").authenticated()                     // 👈 나머지 /auth는 인증 필요 (/auth/logout 등)
                        .anyRequest().permitAll())
                .formLogin(AbstractHttpConfigurer::disable)
                //.oauth2Login(Customizer.withDefaults())
                .addFilterBefore(new TokenExceptionFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
		"http://3.34.92.187.nip.io",
		"https://3.34.92.187.nip.io",
                "http://localhost:5173",
                "http://3.34.92.187.nip.io:5173",
                "http://3.34.92.187:5173",
                "https://74ca-113-198-83-192.ngrok-free.app",
                "https://3.34.92.187.nip.io:5173",
                "https://3.34.92.187:5173",
                "https://mymeeting-backend.loca.lt"
                )); // React 프론트엔드 주소
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
