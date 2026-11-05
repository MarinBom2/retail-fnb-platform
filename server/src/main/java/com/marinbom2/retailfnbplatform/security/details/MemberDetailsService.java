package com.marinbom2.retailfnbplatform.security.details;

import com.marinbom2.retailfnbplatform.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** 로그인 ID(이메일)로 Member 를 찾는다. Member 가 UserDetails 라 변환 없이 그대로 돌려준다 */
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원이 없습니다"));
    }
}
