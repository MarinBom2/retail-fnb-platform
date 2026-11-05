package com.marinbom2.retailfnbplatform.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InsufficientStockException;
import com.marinbom2.retailfnbplatform.order.domain.exception.IllegalOrderStateException;
import com.marinbom2.retailfnbplatform.store.domain.exception.IllegalStoreStateException;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러 단계 오류를 ProblemDetail + code 로 응답한다 (develop-principal.md §7).
 * 도메인 예외 메시지와 스택은 응답에 넣지 않고 로그로만 남긴다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        log.info("business error: {} {}", e.getErrorCode(), e.getMessage());
        return ProblemDetails.of(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException e) {
        log.info("out of stock: {}", e.getMessage());
        ProblemDetail problem = ProblemDetails.of(ErrorCode.OUT_OF_STOCK);
        // 화면이 모자란 상품을 짚을 수 있게 SKU ID 만 내보낸다 (수량은 내보내지 않는다)
        problem.setProperty("skuId", e.getSkuId());
        return problem;
    }

    @ExceptionHandler(IllegalOrderStateException.class)
    public ProblemDetail handleIllegalOrderState(IllegalOrderStateException e) {
        log.info("order state conflict: {}", e.getMessage());
        return ProblemDetails.of(ErrorCode.ORDER_STATE_CONFLICT);
    }

    @ExceptionHandler(IllegalStoreStateException.class)
    public ProblemDetail handleIllegalStoreState(IllegalStoreStateException e) {
        log.info("store state conflict: {}", e.getMessage());
        return ProblemDetails.of(ErrorCode.STORE_STATE_CONFLICT);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException e) {
        log.info("concurrent update: {}", e.getMessage());
        return ProblemDetails.of(ErrorCode.CONCURRENT_UPDATE);
    }

    /** 위에서 따로 다루지 않는 도메인 규칙 위반. 메시지(내부 정보)는 로그로만 남긴다 */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException e) {
        log.info("domain rule violation: {} {}", e.getClass().getSimpleName(), e.getMessage());
        return ProblemDetails.of(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return ProblemDetails.of(ErrorCode.INTERNAL_ERROR);
    }

    /** @Valid 검증 실패: 필드별 오류를 errors 로 함께 내보낸다 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.INVALID_REQUEST_DATA);
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", String.valueOf(fieldError.getDefaultMessage())))
                .toList();
        problem.setProperty(ProblemDetails.ERRORS, errors);
        return handleExceptionInternal(e, problem, headers, status, request);
    }

    /**
     * 부모 클래스가 처리하는 Spring MVC 예외 전부가 여기를 지난다.
     * 부모는 본문을 비운 채(null) 넘기고 여기서 ProblemDetail 을 만들기 때문에, 부모가 응답을 만든 뒤에 손본다.
     * code 가 없는 응답에는 상태에 맞는 기본 code 와 한글 문구를 입힌다. Spring 의 영문 detail 은 로그로만 남긴다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(e, body, headers, status, request);
        if (response != null
                && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(ProblemDetails.CODE))) {
            ErrorCode errorCode = ErrorCode.defaultFor(status.value());
            log.debug("spring mvc error {}: {}", status.value(), problem.getDetail());
            problem.setTitle(HttpStatus.valueOf(status.value()).getReasonPhrase());
            problem.setDetail(errorCode.getDescription());
            problem.setProperty(ProblemDetails.CODE, errorCode.getCode());
        }
        return response;
    }
}
