package com.marinbom2.retailfnbplatform.exception;

import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import lombok.Getter;

/**
 * 애플리케이션 계층 공통 예외. GlobalExceptionHandler 가 ProblemDetail 로 바꾼다 (develop-principal.md §7).
 * 메시지는 화면에 그대로 나가므로 내부 정보를 넣지 않는다. 도메인 규칙 위반은 도메인 예외를 쓴다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDescription());
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
