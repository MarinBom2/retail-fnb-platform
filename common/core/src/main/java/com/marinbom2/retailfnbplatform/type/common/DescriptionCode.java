package com.marinbom2.retailfnbplatform.type.common;

/**
 * 코드(enum 이름)와 한글 설명을 함께 내보내는 enum 의 공통 규약.
 * {@code @JsonFormat(shape = Shape.OBJECT)}와 같이 쓰면 {"code": "USER", "description": "유저"} 형태로 직렬화된다.
 * 서버의 모든 enum 이 구현한다 (md/develop-principal.md §1).
 */
public interface DescriptionCode {

    default String getCode() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }
        return null;
    }

    String getDescription();
}
