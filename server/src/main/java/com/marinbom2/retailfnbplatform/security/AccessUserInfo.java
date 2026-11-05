package com.marinbom2.retailfnbplatform.security;

import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 컨트롤러 밖에서 로그인 회원을 꺼낸다. 컨트롤러는 @AuthenticationPrincipal 로 받는다.
 * 준영속 객체라 값을 바꿀 때는 ID 로 다시 읽는다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccessUserInfo {

    /** @throws BusinessException INVALID_ACCESS_TOKEN. 인증 정보가 없을 때 */
    public static Member getLoginMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Member member) {
            return member;
        }
        throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, "인증되지 않은 사용자입니다.");
    }
}
