package com.marinbom2.retailfnbplatform.security.provider;

import com.marinbom2.retailfnbplatform.security.token.MemberAuthenticationToken;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 이메일·비밀번호 검증 (FR-AU-002). 계정 없음과 불일치를 같은 예외로 던져 존재 여부를 숨긴다.
 * bypassEmails 는 local 전용 백도어 (SecurityConfig 가 채운다).
 */
@Slf4j
public class MemberAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService memberDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> bypassEmails;
    private final String dummyPasswordHash;

    public MemberAuthenticationProvider(UserDetailsService memberDetailsService,
                                        PasswordEncoder passwordEncoder,
                                        Set<String> bypassEmails) {
        this.memberDetailsService = memberDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.bypassEmails = Set.copyOf(bypassEmails);
        this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer");

        if (!this.bypassEmails.isEmpty()) {
            log.warn("[login] 비밀번호 우회 로그인이 켜져 있습니다. emails={} (local 전용)", this.bypassEmails);
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        UserDetails member;
        try {
            member = memberDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new BadCredentialsException("Invalid account or password");
        }

        if (bypassEmails.contains(email)) {
            log.warn("[LOGIN BYPASS] email={} 비밀번호 검증 건너뜀 (local 전용)", email);
            return authenticated(member);
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BadCredentialsException("Invalid account or password");
        }
        return authenticated(member);
    }

    /** 인증 완료 토큰. principal 은 Member(UserDetails), 비밀번호는 담지 않는다 */
    private static Authentication authenticated(UserDetails member) {
        return UsernamePasswordAuthenticationToken.authenticated(member, null, member.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MemberAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
