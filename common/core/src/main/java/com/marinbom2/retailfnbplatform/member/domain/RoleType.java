package com.marinbom2.retailfnbplatform.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 회원 역할 (FR-AU-003). 관리자 API(/api/v1/admin/**)는 {@link #isAdmin()}인 역할만 들어간다. */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum RoleType implements DescriptionCode {

    USER("유저"),
    ADMIN("관리자"),
    SYSTEM("시스템 관리자");

    private final String description;

    /** 관리자 권한인지 (ADMIN, SYSTEM) */
    public boolean isAdmin() {
        return this == ADMIN || this == SYSTEM;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
