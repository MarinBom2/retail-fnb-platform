package com.marinbom2.retailfnbplatform.inventory.api.dto;

import com.marinbom2.retailfnbplatform.inventory.domain.MovementType;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovement;
import com.marinbom2.retailfnbplatform.inventory.domain.StockReferenceType;
import java.time.Instant;

public record StockMovementResponse(
        Long movementId,
        MovementType type,
        long delta,
        long balanceAfter,
        StockReferenceType referenceType,
        Long referenceId,
        Instant createdAt
) {

    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(m.getId(), m.getType(), m.getDelta(), m.getBalanceAfter(),
                m.getReference().type(), m.getReference().id(), m.getCreatedAt());
    }
}
