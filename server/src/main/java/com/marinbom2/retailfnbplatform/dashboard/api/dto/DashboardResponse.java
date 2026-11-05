package com.marinbom2.retailfnbplatform.dashboard.api.dto;

import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import java.time.LocalDate;
import java.util.List;

/** 오늘 매장 현황 (md/api.md §2) */
public record DashboardResponse(
        Long storeId,
        LocalDate businessDate,
        long salesAmount,
        long orderCount,
        long averageTicket,
        long refundedAmount,
        List<HourlySales> hourlySales,
        List<TopItem> topItems,
        List<LowStockItem> lowStock,
        long openOrderCount,
        long transfersToApprove,
        long transfersToShip,
        long transfersToReceive
) {

    public record HourlySales(int hour, long amount) {
    }

    public record TopItem(Long skuId, String itemName, long quantity, long amount) {
    }

    public record LowStockItem(
            Long skuId,
            String itemName,
            long quantity,
            long reorderPoint,
            StockUnit unit,
            Long bestSourceStoreId,
            String bestSourceStoreName,
            Long bestSourceQuantity
    ) {
    }
}
