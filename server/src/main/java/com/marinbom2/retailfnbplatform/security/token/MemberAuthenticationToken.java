package com.marinbom2.retailfnbplatform.security.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * 로그인 요청(이메일·비밀번호)을 담는 인증 전 토큰.
 * 별도 타입으로 둬서 ProviderManager 가 MemberAuthenticationProvider 로만 보내게 한다 ({@code supports}).
 */
public class MemberAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private MemberAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials); // 인자 두 개 생성자는 authenticated=false
    }

    public static MemberAuthenticationToken unauthenticated(String email, String password) {
        return new MemberAuthenticationToken(email, password);
    }
}
