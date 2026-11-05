package com.marinbom2.retailfnbplatform.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 의류 SKU 옵션. 색상·사이즈 둘 다 있어야 한다. 표시 문구는 "BLACK / L" */
@Embeddable
public record ApparelOption(
        @Column(name = "option_color", length = 30) String color,
        @Column(name = "option_size", length = 10) String size
) {

    public ApparelOption {
        if (color == null || color.isBlank() || size == null || size.isBlank()) {
            throw new IllegalArgumentException("색상과 사이즈가 모두 필요합니다");
        }
        color = color.trim().toUpperCase();
        size = size.trim().toUpperCase();
    }

    public String label() {
        return color + " / " + size;
    }
}
