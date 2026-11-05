package com.marinbom2.retailfnbplatform.domain.base;

/**
 * 도메인 규칙 위반 예외의 공통 부모. 모두 unchecked 라 트랜잭션이 자동 롤백된다.
 * 메시지는 로그용이다. 화면 문구는 GlobalExceptionHandler 가 ErrorCode 로 바꿔 내보낸다 (md/develop-principal.md §2).
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
