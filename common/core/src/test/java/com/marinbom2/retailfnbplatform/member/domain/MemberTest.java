package com.marinbom2.retailfnbplatform.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class MemberTest {

    @Test
    @DisplayName("가입: 이메일은 소문자·공백 제거, 권한 USER, 토큰 버전 0 (FR-AU-001)")
    void signUp() {
        Member member = Member.signUp("  Tester@Example.COM ", "{bcrypt}hash");

        assertThat(member.getEmail()).isEqualTo("tester@example.com");
        assertThat(member.getRoleType()).isEqualTo(RoleType.USER);
        assertThat(member.getTokenVersion()).isZero();
    }

    @Test
    @DisplayName("로그아웃하면 토큰 버전이 올라가 이전 버전 토큰이 무효가 된다 (FR-AU-002)")
    void invalidateTokens() {
        Member member = Member.signUp("a@example.com", "{bcrypt}hash");

        member.invalidateTokens();

        assertThat(member.isCurrentTokenVersion(0)).isFalse();
        assertThat(member.isCurrentTokenVersion(1)).isTrue();
    }

    @Test
    @DisplayName("UserDetails: 로그인 ID 는 이메일, 권한은 RoleType 이름 그대로 (ROLE_ 접두사 없음)")
    void userDetails() {
        Member member = Member.signUp("a@example.com", "{bcrypt}hash");
        member.changeRole(RoleType.ADMIN);

        assertThat(member.getUsername()).isEqualTo("a@example.com");
        assertThat(member.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(member.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ADMIN");
        assertThat(member.isAdmin()).isTrue();
        assertThat(member.isEnabled()).isTrue();
    }
}
