package com.marinbom2.retailfnbplatform.exception;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 컨트롤러 단계 오류가 ProblemDetail(RFC 9457) + code 로 나가는지 (md/develop-principal.md §7) */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException: ErrorCode 의 상태·code·문구, 확장 필드는 최상위")
    void businessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND_DATA"))
                .andExpect(jsonPath("$.detail").value(ErrorCode.NOT_FOUND_DATA.getDescription()))
                .andExpect(jsonPath("$.instance").value("/test/business"))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    @DisplayName("@Valid 실패: INVALID_REQUEST_DATA + 필드별 errors")
    void validation() throws Exception {
        mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("Spring MVC 예외(지원하지 않는 메서드): 기본 code 와 한글 문구를 입힌다")
    void springMvcException() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").value(ErrorCode.METHOD_NOT_ALLOWED.getDescription()));
    }

    @Test
    @DisplayName("예상하지 못한 예외: 500 INTERNAL_ERROR, 예외 메시지는 나가지 않는다")
    void unexpected() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value(not(containsString("secret"))));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(ErrorCode.NOT_FOUND_DATA);
        }

        @PostMapping("/test/valid")
        String valid(@Valid @RequestBody NameRequest request) {
            return request.name();
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    record NameRequest(@NotBlank String name) {
    }
}
