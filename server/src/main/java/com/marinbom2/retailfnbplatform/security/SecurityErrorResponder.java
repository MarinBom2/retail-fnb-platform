package com.marinbom2.retailfnbplatform.security;

import com.marinbom2.retailfnbplatform.exception.ProblemDetails;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시큐리티 단계 오류(401, 403, 로그인 실패)를 ProblemDetail 로 쓰는 유일한 곳 (develop-principal.md §7).
 * 필터는 컨트롤러 앞에서 돌아 GlobalExceptionHandler 가 받지 못하기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler, AuthenticationFailureHandler {

    /** JWT 필터가 실패 이유(ErrorCode)를 남기는 요청 속성. 없으면 UNAUTHORIZED */
    public static final String ERROR_CODE_ATTRIBUTE = SecurityErrorResponder.class.getName() + ".errorCode";

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        ErrorCode errorCode = request.getAttribute(ERROR_CODE_ATTRIBUTE) instanceof ErrorCode reason
                ? reason
                : ErrorCode.UNAUTHORIZED;
        write(request, response, errorCode);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        write(request, response, ErrorCode.FORBIDDEN);
    }

    /** 로그인 실패. 본문 형식 문제(AuthenticationServiceException)와 계정·비밀번호 불일치를 나눈다 */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        ErrorCode errorCode = e instanceof AuthenticationServiceException
                ? ErrorCode.INVALID_REQUEST_DATA
                : ErrorCode.LOGIN_FAILED;
        write(request, response, errorCode);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        ProblemDetail problem = ProblemDetails.of(errorCode, errorCode.getDescription(), request.getRequestURI());
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
