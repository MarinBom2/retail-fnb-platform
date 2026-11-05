package com.marinbom2.retailfnbplatform.security.jwt;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    /**
     * 운영(prod)에서 jwt.secret 이 비어 있으면 기동을 멈춘다.
     * 임시 키로 뜨면 블루그린 전환 때 두 컨테이너의 키가 달라 로그인이 전부 풀린다 (aws-infra-deploy.md 8절).
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties properties, Clock clock, Environment environment) {
        boolean blankSecret = properties.secret() == null || properties.secret().isBlank();
        if (blankSecret && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("운영 프로필에서는 jwt.secret(JWT_SECRET 환경변수)이 필요합니다");
        }
        return new JwtTokenProvider(properties.secret(), properties.accessTokenTtl(), clock);
    }
}
