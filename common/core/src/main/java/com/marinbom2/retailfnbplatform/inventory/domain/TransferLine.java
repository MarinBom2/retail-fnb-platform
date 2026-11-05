package com.marinbom2.retailfnbplatform.inventory.domain;

import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InvalidTransferException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** 이동할 SKU 하나와 수량. 이름은 요청한 순간의 값으로 남긴다 */
@Embeddable
public record TransferLine(
        @Column(name = "sku_id", nullable = false) Long skuId,
        @Column(name = "item_name", nullable = false, length = 250) String itemName,
        @Column(nullable = false) long quantity
) {

    public static TransferLine of(Sku sku, long quantity) {
        if (!sku.tracksStock()) {
            throw new InvalidTransferException("재고를 세지 않는 SKU 는 이동할 수 없습니다: " + sku.getSkuCode());
        }
        if (quantity <= 0) {
            throw new InvalidTransferException("이동 수량은 1 이상이어야 합니다");
        }
        return new TransferLine(Objects.requireNonNull(sku.getId(), "저장된 SKU 여야 합니다"), sku.displayName(), quantity);
    }
}
