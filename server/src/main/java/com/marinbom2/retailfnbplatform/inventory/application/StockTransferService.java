package com.marinbom2.retailfnbplatform.inventory.application;

import com.marinbom2.retailfnbplatform.catalog.application.CatalogLookup;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.inventory.api.dto.TransferRequest;
import com.marinbom2.retailfnbplatform.inventory.api.dto.TransferResponse;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovementRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockReference;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockTransfer;
import com.marinbom2.retailfnbplatform.inventory.domain.StockTransferRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferLine;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InsufficientStockException;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.application.StoreAccess;
import com.marinbom2.retailfnbplatform.store.application.StoreAccessGuard;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매장 간 이동: 요청 → 승인 → 출고 → 입고 (FR-IN-003, md/api.md §5).
 * 어느 매장이 어떤 동작을 할 수 있는지는 StockTransfer 가 정하고, 여기서는 재고 권한과 재고 변동만 맡는다.
 * 출고 때 보낼 매장 재고가, 입고 때 받을 매장 재고가 같은 트랜잭션에서 바뀐다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StoreAccessGuard storeAccessGuard;
    private final CatalogLookup catalogLookup;
    private final StoreRepository storeRepository;
    private final StockTransferRepository transferRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final Clock clock;

    /** 받을 매장(경로)이 보낼 매장에 요청한다 */
    @Transactional
    public TransferResponse request(Long storeId, TransferRequest request, Member member) {
        StoreAccess access = storeAccessGuard.requireInventoryManager(storeId, member);
        Store from = storeRepository.findById(request.fromStoreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "보낼 매장을 찾을 수 없습니다."));
        Map<Long, Sku> skus = catalogLookup.skusOf(access.store(), request.lines().stream().map(TransferRequest.Line::skuId).toList())
                .stream().collect(Collectors.toMap(Sku::getId, Function.identity()));
        List<TransferLine> lines = request.lines().stream()
                .map(line -> TransferLine.of(skus.get(line.skuId()), line.quantity()))
                .toList();

        StockTransfer transfer = transferRepository.save(
                StockTransfer.request(from, access.store(), lines, request.memo(), member.getId(), clock.instant()));
        log.info("[transfer] requested transferId={} from={} to={}", transfer.getId(), from.getId(), storeId);
        return response(transfer, access);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> list(Long storeId, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        List<StockTransfer> transfers = transferRepository.findTop50ByFromStoreIdOrToStoreIdOrderByIdDesc(storeId, storeId);
        Map<Long, String> names = storeNames(transfers);
        return transfers.stream()
                .map(t -> TransferResponse.of(t, storeId, access.canManageInventory(), names))
                .toList();
    }

    @Transactional
    public TransferResponse approve(Long storeId, Long transferId, Member member) {
        return act(storeId, transferId, member, (transfer, access) -> transfer.approve(storeId, clock.instant()));
    }

    @Transactional
    public TransferResponse reject(Long storeId, Long transferId, String reason, Member member) {
        return act(storeId, transferId, member, (transfer, access) -> transfer.reject(storeId, reason, clock.instant()));
    }

    @Transactional
    public TransferResponse cancel(Long storeId, Long transferId, Member member) {
        return act(storeId, transferId, member, (transfer, access) -> transfer.cancel(storeId, clock.instant()));
    }

    /** @throws InsufficientStockException 보낼 매장 재고가 모자라면. 상태도 APPROVED 로 남는다 (롤백) */
    @Transactional
    public TransferResponse ship(Long storeId, Long transferId, Member member) {
        return act(storeId, transferId, member, (transfer, access) -> {
            transfer.ship(storeId, clock.instant());
            StockReference reference = StockReference.transfer(transfer.getId());
            transfer.getLines().forEach(line -> {
                Stock stock = stockRepository.findByStoreIdAndSkuId(storeId, line.skuId())
                        .orElseThrow(() -> new InsufficientStockException(storeId, line.skuId(), 0, line.quantity()));
                stockMovementRepository.save(stock.transferOut(line.quantity(), reference));
            });
        });
    }

    @Transactional
    public TransferResponse receive(Long storeId, Long transferId, Member member) {
        return act(storeId, transferId, member, (transfer, access) -> {
            transfer.receive(storeId, clock.instant());
            StockReference reference = StockReference.transfer(transfer.getId());
            transfer.getLines().forEach(line -> {
                Stock stock = stockRepository.findByStoreIdAndSkuId(storeId, line.skuId())
                        .orElseGet(() -> stockRepository.save(Stock.empty(storeId, line.skuId())));
                stockMovementRepository.save(stock.transferIn(line.quantity(), reference));
            });
        });
    }

    private TransferResponse act(Long storeId, Long transferId, Member member, TransferStep step) {
        StoreAccess access = storeAccessGuard.requireInventoryManager(storeId, member);
        StockTransfer transfer = transferRepository.findById(transferId)
                .filter(t -> t.involves(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "이동 건을 찾을 수 없습니다."));
        step.apply(transfer, access);
        log.info("[transfer] {} transferId={} by storeId={}", transfer.getStatus(), transferId, storeId);
        return response(transfer, access);
    }

    private TransferResponse response(StockTransfer transfer, StoreAccess access) {
        return TransferResponse.of(transfer, access.storeId(), access.canManageInventory(), storeNames(List.of(transfer)));
    }

    private Map<Long, String> storeNames(List<StockTransfer> transfers) {
        Set<Long> ids = transfers.stream()
                .flatMap(t -> Stream.of(t.getFromStoreId(), t.getToStoreId()))
                .collect(Collectors.toSet());
        return storeRepository.findAllById(ids).stream().collect(Collectors.toMap(Store::getId, Store::getName));
    }

    @FunctionalInterface
    private interface TransferStep {
        void apply(StockTransfer transfer, StoreAccess access);
    }
}
