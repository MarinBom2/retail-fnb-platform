package com.marinbom2.retailfnbplatform.exception;

import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.net.URI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * ErrorCode 로 ProblemDetail 을 만드는 유일한 곳. 컨트롤러 단계와 시큐리티 단계가 같이 쓴다.
 * 객체만 만들고 JSON 변환은 Boot 의 JsonMapper 가 한다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProblemDetails {

    /** 클라이언트가 분기에 쓰는 확장 필드 이름 */
    public static final String CODE = "code";

    /** 입력값 검증 실패 때 필드별 오류 목록 */
    public static final String ERRORS = "errors";

    public static ProblemDetail of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getDescription());
    }

    /** @param detail 화면에 그대로 나가는 문장. 내부 정보를 넣지 않는다 */
    public static ProblemDetail of(ErrorCode errorCode, String detail) {
        HttpStatus status = HttpStatus.valueOf(errorCode.getStatus());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty(CODE, errorCode.getCode());
        return problem;
    }

    /** 컨트롤러 밖(시큐리티 필터)에서는 Spring 이 instance 를 채워 주지 않으므로 직접 넣는다 */
    public static ProblemDetail of(ErrorCode errorCode, String detail, String requestUri) {
        ProblemDetail problem = of(errorCode, detail);
        problem.setInstance(URI.create(requestUri));
        return problem;
    }
}
