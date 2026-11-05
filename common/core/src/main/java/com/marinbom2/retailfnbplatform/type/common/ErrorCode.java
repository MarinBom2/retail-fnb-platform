package com.marinbom2.retailfnbplatform.type.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 오류 코드. ProblemDetail 의 status·code·detail 이 여기서 나온다 (develop-principal.md §7).
 * description 은 화면 문구다. status 는 core 에 spring-web 이 없어 int.
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ErrorCode implements DescriptionCode {

    /* 400 */
    BAD_REQUEST(400, "잘못된 요청입니다."),
    INVALID_REQUEST_DATA(400, "입력값을 다시 확인해 주세요."),
    /** 도메인 규칙 위반 (업종에 맞지 않는 상품·주문 방식, 결제 금액 불일치 등) */
    DOMAIN_RULE_VIOLATION(400, "처리할 수 없는 요청입니다. 입력값을 다시 확인해 주세요."),

    /* 401 */
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    INVALID_ACCESS_TOKEN(401, "로그인 정보가 유효하지 않습니다. 다시 로그인해 주세요."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 일치하지 않습니다."),

    /* 403 */
    FORBIDDEN(403, "요청할 권한이 없습니다."),

    /* 404 */
    NOT_FOUND(404, "올바른 경로가 아닙니다."),
    NOT_FOUND_DATA(404, "요청한 정보를 찾을 수 없습니다."),
    NOT_FOUND_MEMBER(404, "회원 정보를 찾을 수 없습니다."),

    /* 405 */
    METHOD_NOT_ALLOWED(405, "지원하지 않는 요청 방식입니다."),

    /* 409 */
    CONFLICT(409, "요청이 현재 상태와 충돌합니다."),
    ALREADY_EXIST(409, "이미 존재합니다."),
    STORE_STATE_CONFLICT(409, "지금 매장 상태에서는 할 수 없는 요청입니다."),
    ORDER_STATE_CONFLICT(409, "지금 주문 상태에서는 할 수 없는 요청입니다."),
    OUT_OF_STOCK(409, "재고가 부족합니다."),
    /** 같은 재고·주문을 동시에 바꿨을 때 (낙관적 락). 다시 시도하면 된다 */
    CONCURRENT_UPDATE(409, "다른 요청과 겹쳤습니다. 다시 시도해 주세요."),

    /* 413 */
    PAYLOAD_TOO_LARGE(413, "파일 크기가 너무 큽니다."),

    /* 429 */
    WAIT_REQUEST_TIMEOUT(429, "잠시 후 다시 시도해 주세요."),

    /* 500 */
    INTERNAL_ERROR(500, "서버에서 오류가 발생했습니다."),

    /* 503 */
    SERVICE_UNAVAILABLE(503, "지금은 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final int status;
    private final String description;

    @Override
    public String getDescription() {
        return description;
    }

    /** 상태 코드만 있는 오류(Spring 이 직접 낸 404, 405 등)에 붙일 기본 코드 */
    public static ErrorCode defaultFor(int status) {
        return switch (status) {
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 429 -> WAIT_REQUEST_TIMEOUT;
            case 503 -> SERVICE_UNAVAILABLE;
            default -> status >= 500 ? INTERNAL_ERROR : BAD_REQUEST;
        };
    }
}
