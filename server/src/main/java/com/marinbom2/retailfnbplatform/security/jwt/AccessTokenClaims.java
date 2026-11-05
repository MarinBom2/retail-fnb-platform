package com.marinbom2.retailfnbplatform.security.jwt;

/**
 * 검증을 통과한 액세스 토큰의 내용.
 *
 * @param memberId     Member.id (JWT sub)
 * @param tokenVersion 발급 당시 Member.tokenVersion. 지금 값과 다르면 로그아웃된 토큰이다
 */
public record AccessTokenClaims(long memberId, int tokenVersion) {
}
