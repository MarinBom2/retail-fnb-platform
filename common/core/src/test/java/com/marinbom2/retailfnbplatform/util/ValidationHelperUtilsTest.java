package com.marinbom2.retailfnbplatform.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationHelperUtilsTest {

    @Test
    @DisplayName("비밀번호 길이 경계: 5자·21자는 거부, 6자·20자는 통과")
    void passwordLength() {
        assertThat(ValidationHelperUtils.isInValidPassword("Abc12")).isTrue();
        assertThat(ValidationHelperUtils.isInValidPassword("Abc123")).isFalse();
        assertThat(ValidationHelperUtils.isInValidPassword("Abc1234567890123456x")).isFalse();
        assertThat(ValidationHelperUtils.isInValidPassword("Abc1234567890123456xy")).isTrue();
        assertThat(ValidationHelperUtils.isInValidPassword(null)).isTrue();
    }

    @Test
    @DisplayName("로그용 이메일은 첫 글자와 도메인만 남긴다")
    void maskEmail() {
        assertThat(ValidationHelperUtils.maskEmail("tester@example.com")).isEqualTo("t***@example.com");
        assertThat(ValidationHelperUtils.maskEmail("@x")).isEqualTo("***");
    }
}
