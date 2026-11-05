package com.marinbom2.retailfnbplatform.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 회원 인증의 개발 편의 설정 ({@code security.*}). 기본값은 꺼짐이고 application-local.yml 에서만 켠다.
 *
 * @param loginBypass 비밀번호 검증을 건너뛰는 개발용 백도어. local 프로필이면서 목록이 있을 때만 동작한다
 */
@ConfigurationProperties("security")
public record MemberSecurityProperties(
        @DefaultValue LoginBypass loginBypass
) {

    /** @param emails 비밀번호 없이 로그인을 허용할 이메일 (가입은 되어 있어야 한다) */
    public record LoginBypass(@DefaultValue List<String> emails) {
    }
}
