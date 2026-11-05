package com.marinbom2.retailfnbplatform.catalog.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 판매 상태. ON_SALE ⇄ SUSPENDED → DISCONTINUED (단종은 되돌리지 않는다) */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ProductStatus implements DescriptionCode {

    ON_SALE("판매 중"),
    SUSPENDED("판매 중지"),
    DISCONTINUED("단종");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
