package com.marinbom2.retailfnbplatform.catalog.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;

/** 상품·SKU·레시피 규칙 위반 (업종에 맞지 않는 종류, 옵션 누락·중복 등) */
public class InvalidCatalogException extends DomainException {

    public InvalidCatalogException(String message) {
        super(message);
    }
}
