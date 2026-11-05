package com.marinbom2.retailfnbplatform.member.api.dto;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.member.domain.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

/** 회원 정보 응답. 로그인 성공, 가입, 내 정보에서 같이 쓴다 (비밀번호·토큰 버전은 내보내지 않는다) */
@Schema(description = "회원 정보")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1") Long memberId,
        @Schema(description = "이메일(로그인 ID)", example = "tester@example.com") String email,
        @Schema(description = "권한. {code, description} 객체로 온다") RoleType roleType
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getRoleType());
    }
}
