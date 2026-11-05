package com.marinbom2.retailfnbplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseEntity 의 created_at·modified_at 을 채운다.
 * 앱 클래스(@SpringBootApplication)에 붙이지 않고 따로 둔다. 거기 붙이면 JPA 가 없는 슬라이스 테스트(@WebMvcTest 등)가 깨진다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
