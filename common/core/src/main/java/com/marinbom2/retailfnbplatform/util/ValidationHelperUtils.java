package com.marinbom2.retailfnbplatform.util;

import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationHelperUtils {

    /** 비밀번호 6~20자. 영문 대문자·소문자·숫자·특수문자 중 두 종류 이상 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?:(?=.*[a-z])(?=.*[A-Z])|(?=.*[a-z])(?=.*\\d)|(?=.*[A-Z])(?=.*\\d)|(?=.*[@#$%^&+=!~*()_\\-{}\\[\\]:;\"'<>,.?\\/\\\\]))"
                    + "[a-zA-Z\\d@#$%^&+=!~*()_\\-{}\\[\\]:;\"'<>,.?\\/\\\\]{6,20}$");

    public static boolean isInValidPassword(String password) {
        return password == null || !PASSWORD_PATTERN.matcher(password).matches();
    }

    /** 로그용 이메일 가리기: tester@example.com → t***@example.com */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
