package com.marinbom2.retailfnbplatform.member.api;

import com.marinbom2.retailfnbplatform.member.api.dto.MemberResponse;
import com.marinbom2.retailfnbplatform.member.application.MemberService;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 회원 본인의 정보 */
@Tag(name = "내 정보", description = "세션 확인, 내 정보")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final MemberService memberService;

    @Operation(summary = "내 정보",
            description = "FR-AU-002. 로그인 상태 확인에도 쓴다. 인증 쿠키가 없거나 무효면 401")
    @GetMapping
    public MemberResponse me(@AuthenticationPrincipal Member member) {
        return MemberResponse.from(memberService.get(member.getId()));
    }
}
