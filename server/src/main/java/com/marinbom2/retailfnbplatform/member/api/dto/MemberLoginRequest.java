package com.marinbom2.retailfnbplatform.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 요청 본문. 실제 처리는 시큐리티 필터(MemberAuthenticationAPIFilter)가 JSON 을 직접 읽는다.
 * 이 타입은 Swagger 문서(OpenApiConfig)에 요청 형태를 보여 주는 데만 쓴다.
 */
@Schema(description = "로그인 요청")
public record MemberLoginRequest(
        @Schema(description = "이메일", example = "tester@example.com") String email,
        @Schema(description = "비밀번호", example = "Passw0rd!") String password
) {
}
