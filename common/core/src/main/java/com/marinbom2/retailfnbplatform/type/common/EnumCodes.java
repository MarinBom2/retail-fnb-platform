package com.marinbom2.retailfnbplatform.type.common;

import com.marinbom2.retailfnbplatform.exception.BusinessException;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 요청의 enum code 문자열을 enum 으로. 모르는 값은 400 INVALID_REQUEST_DATA (500 이 나지 않게) */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnumCodes {

    public static <E extends Enum<E>> E parse(Class<E> type, String code) {
        return Arrays.stream(type.getEnumConstants())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST_DATA, "지원하지 않는 값입니다: " + code));
    }
}
