package com.marinbom2.retailfnbplatform.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각은 이 Clock 빈으로 얻는다 (Instant.now() 를 직접 부르지 않는다).
 * 테스트에서 고정 시각으로 바꿔 만료·24h 파기 같은 시간 규칙을 검증할 수 있다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
