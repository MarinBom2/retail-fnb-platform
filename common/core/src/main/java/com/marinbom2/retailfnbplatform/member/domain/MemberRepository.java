package com.marinbom2.retailfnbplatform.member.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** 로그인·인증 주체 조회 */
    Optional<Member> findByEmail(String email);

    /** 가입 중복 확인 */
    boolean existsByEmail(String email);
}
