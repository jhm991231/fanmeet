package dev.fanmeet.config;

import dev.fanmeet.auth.CustomOAuth2UserService;
import dev.fanmeet.auth.JwtAuthenticationFilter;
import dev.fanmeet.auth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.front-url}")
    private String frontUrl;
    
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // JWT를 쓸 것이므로 서버 세션을 만들지 않는다
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 세션 쿠키 기반이 아니므로 CSRF 보호 대상이 아님 (refresh 쿠키는 SameSite로 방어)
                .csrf(csrf -> csrf.disable())
                // 기본 로그인 폼/브라우저 팝업 인증은 사용하지 않는다
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler))
                // 미인증 요청은 로그인 페이지로 보내지 않고 401로 답한다 (API 서버이므로)
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 이 둘은 access 토큰이 없는 상태에서 부르는 API 다. 만료돼서
                        // 재발급하는 것이므로 인증을 요구하면 모순이다. 인증은 필터가 아니라
                        // 핸들러 안에서 쿠키의 refresh 토큰으로 대신한다.
                        //
                        // "/api/auth/**" 로 열지 않은 것은 의도적이다. 와일드카드는 경로
                        // 공간 전체를 열어, 나중에 그 아래 추가되는 엔드포인트까지 아무도
                        // 모르게 공개된다. 메서드까지 지정해 범위를 더 좁혔다.
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh", "/api/auth/logout")
                        .permitAll()
                        .anyRequest().authenticated())
                // 아이디/비번 인증 자리에 우리 JWT 검문원을 대신 배치한다
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // refresh 쿠키를 주고받아야 하므로 필수
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
