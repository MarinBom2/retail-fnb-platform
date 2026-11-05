package com.marinbom2.retailfnbplatform.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 가입 요청 (FR-AU-001). 비밀번호 규칙은 서비스에서 ValidationHelperUtils 로 본다 */
@Schema(description = "가입 요청")
public record MemberSignupRequest(
        @Schema(description = "이메일(로그인 ID)", example = "tester@example.com")
        @NotBlank @Email @Size(max = 100) String email,

        @Schema(description = "비밀번호. 6~20자, 영문 대소문자·숫자·특수문자 조합", example = "Passw0rd!")
        @NotBlank String password
) {
}
