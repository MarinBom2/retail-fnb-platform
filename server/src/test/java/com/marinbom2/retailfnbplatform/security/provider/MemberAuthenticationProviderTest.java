package com.marinbom2.retailfnbplatform.security.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.security.token.MemberAuthenticationToken;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberAuthenticationProviderTest {

    private static final String EMAIL = "tester@example.com";
    private static final String PASSWORD = "Passw0rd!";

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final Member member = Member.signUp(EMAIL, passwordEncoder.encode(PASSWORD));
    private final UserDetailsService detailsService = email -> {
        if (EMAIL.equals(email)) {
            return member;
        }
        throw new UsernameNotFoundException(email);
    };

    @Test
    @DisplayName("비밀번호가 맞으면 principal 이 Member 인 인증 완료 토큰 (비밀번호는 담지 않음)")
    void success() {
        Authentication result = provider(Set.of()).authenticate(MemberAuthenticationToken.unauthenticated(EMAIL, PASSWORD));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(member);
        assertThat(result.getCredentials()).isNull();
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("USER");
    }

    @Test
    @DisplayName("비밀번호 불일치와 없는 계정은 같은 예외 (계정 존재 여부가 새지 않게)")
    void badCredentials() {
        MemberAuthenticationProvider provider = provider(Set.of());

        assertThatThrownBy(() -> provider.authenticate(MemberAuthenticationToken.unauthenticated(EMAIL, "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> provider.authenticate(MemberAuthenticationToken.unauthenticated("nobody@example.com", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("개발용 백도어 목록에 있는 계정은 비밀번호를 보지 않는다")
    void bypass() {
        Authentication result = provider(Set.of(EMAIL))
                .authenticate(MemberAuthenticationToken.unauthenticated(EMAIL, "anything"));

        assertThat(result.getPrincipal()).isSameAs(member);
    }

    @Test
    @DisplayName("MemberAuthenticationToken 만 맡는다 (다른 토큰은 ProviderManager 가 다른 프로바이더로)")
    void supports() {
        MemberAuthenticationProvider provider = provider(Set.of());

        assertThat(provider.supports(MemberAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    private MemberAuthenticationProvider provider(Set<String> bypassEmails) {
        return new MemberAuthenticationProvider(detailsService, passwordEncoder, bypassEmails);
    }
}
