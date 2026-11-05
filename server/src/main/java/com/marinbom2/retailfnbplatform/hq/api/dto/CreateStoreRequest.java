package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 매장 코드는 브랜드 안에서 하나. 영문 대문자·숫자·하이픈 */
public record CreateStoreRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9-]+", message = "매장 코드는 영문·숫자·하이픈만 씁니다.") String code,
        @NotBlank @Size(max = 100) String name
) {
}
