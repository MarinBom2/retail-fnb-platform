package com.marinbom2.retailfnbplatform.inventory.api.dto;

import com.marinbom2.retailfnbplatform.inventory.domain.StockTransfer;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferAction;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 매장 간 이동 (md/api.md §5). direction·availableActions 는 경로의 매장 기준이다.
 * availableActions 는 enum 객체가 아니라 code 문자열 목록이다 (화면이 버튼을 고르는 키).
 */
public record TransferResponse(
        Long transferId,
        Long fromStoreId,
        String fromStoreName,
        Long toStoreId,
        String toStoreName,
        String direction,
        TransferStatus status,
        List<Line> lines,
        String memo,
        String rejectReason,
        Instant requestedAt,
        Instant approvedAt,
        Instant shippedAt,
        Instant receivedAt,
        Instant closedAt,
        List<String> availableActions
) {

    public record Line(Long skuId, String itemName, long quantity) {
    }

    public static TransferResponse of(StockTransfer t, Long viewerStoreId, boolean canManageInventory, Map<Long, String> storeNames) {
        return new TransferResponse(t.getId(), t.getFromStoreId(), storeNames.get(t.getFromStoreId()),
                t.getToStoreId(), storeNames.get(t.getToStoreId()),
                t.getToStoreId().equals(viewerStoreId) ? "INCOMING" : "OUTGOING",
                t.getStatus(),
                t.getLines().stream().map(l -> new Line(l.skuId(), l.itemName(), l.quantity())).toList(),
                t.getMemo(), t.getRejectReason(),
                t.getRequestedAt(), t.getApprovedAt(), t.getShippedAt(), t.getReceivedAt(), t.getClosedAt(),
                t.availableActions(viewerStoreId, canManageInventory).stream().map(TransferAction::name).toList());
    }
}
