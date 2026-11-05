package com.marinbom2.retailfnbplatform.inventory.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.IllegalTransferStateException;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InvalidTransferException;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 간 재고 이동 (애그리거트 루트). 받을 매장이 요청하고, 보낼 매장이 승인·출고하고, 받을 매장이 입고한다.
 * 상태 전이와 "어느 매장이 어떤 동작을 할 수 있는지"를 이 클래스 한 곳에서 정한다 (화면 버튼과 서버 인가가 같은 규칙을 쓴다).
 * 재고 수량은 출고·입고 때 StockTransferService 가 Stock 으로 바꾼다.
 */
@Getter
@Entity
@Table(name = "stock_transfer", indexes = {
        @Index(name = "ix_stock_transfer_from", columnList = "from_store_id, status"),
        @Index(name = "ix_stock_transfer_to", columnList = "to_store_id, status"),
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Column(name = "from_store_id", nullable = false, updatable = false)
    private Long fromStoreId;

    @Column(name = "to_store_id", nullable = false, updatable = false)
    private Long toStoreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @ElementCollection
    @CollectionTable(name = "stock_transfer_line", joinColumns = @JoinColumn(name = "transfer_id"))
    private List<TransferLine> lines = new ArrayList<>();

    @Column(length = 200)
    private String memo;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    /** 거절·취소로 끝난 시각 */
    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private long version;

    /**
     * 이동 요청. 같은 브랜드의 다른 매장끼리만, SKU 는 한 번씩만.
     *
     * @param to 재고가 필요한 (요청하는) 매장
     */
    public static StockTransfer request(Store from, Store to, List<TransferLine> lines, String memo, Long requestedBy, Instant at) {
        if (!Objects.equals(from.getBrandId(), to.getBrandId())) {
            throw new InvalidTransferException("같은 브랜드 매장끼리만 이동할 수 있습니다");
        }
        if (Objects.equals(from.getId(), to.getId())) {
            throw new InvalidTransferException("보내는 매장과 받는 매장이 같습니다");
        }
        if (lines.isEmpty()) {
            throw new InvalidTransferException("이동할 상품이 없습니다");
        }
        if (new HashSet<>(lines.stream().map(TransferLine::skuId).toList()).size() != lines.size()) {
            throw new InvalidTransferException("같은 상품이 두 번 들어 있습니다");
        }
        StockTransfer transfer = new StockTransfer();
        transfer.brandId = from.getBrandId();
        transfer.fromStoreId = Objects.requireNonNull(from.getId());
        transfer.toStoreId = Objects.requireNonNull(to.getId());
        transfer.status = TransferStatus.REQUESTED;
        transfer.lines.addAll(lines);
        transfer.memo = memo == null || memo.isBlank() ? null : memo.trim();
        transfer.requestedBy = Objects.requireNonNull(requestedBy);
        transfer.requestedAt = Objects.requireNonNull(at);
        return transfer;
    }

    /**
     * 이 매장(viewerStoreId)이 지금 할 수 있는 동작. 재고 권한이 없으면 아무것도 없다.
     * 보낼 매장: 승인·거절(REQUESTED), 출고(APPROVED). 받을 매장: 취소(REQUESTED·APPROVED), 입고(SHIPPED).
     */
    public Set<TransferAction> availableActions(Long viewerStoreId, boolean canManageInventory) {
        Set<TransferAction> actions = EnumSet.noneOf(TransferAction.class);
        if (!canManageInventory) {
            return actions;
        }
        if (fromStoreId.equals(viewerStoreId)) {
            if (status == TransferStatus.REQUESTED) {
                actions.add(TransferAction.APPROVE);
                actions.add(TransferAction.REJECT);
            }
            if (status == TransferStatus.APPROVED) {
                actions.add(TransferAction.SHIP);
            }
        }
        if (toStoreId.equals(viewerStoreId)) {
            if (status == TransferStatus.REQUESTED || status == TransferStatus.APPROVED) {
                actions.add(TransferAction.CANCEL);
            }
            if (status == TransferStatus.SHIPPED) {
                actions.add(TransferAction.RECEIVE);
            }
        }
        return actions;
    }

    public void approve(Long actorStoreId, Instant at) {
        require(actorStoreId, TransferAction.APPROVE);
        this.status = TransferStatus.APPROVED;
        this.approvedAt = at;
    }

    public void reject(Long actorStoreId, String reason, Instant at) {
        require(actorStoreId, TransferAction.REJECT);
        if (reason == null || reason.isBlank()) {
            throw new InvalidTransferException("거절 사유가 필요합니다");
        }
        this.status = TransferStatus.REJECTED;
        this.rejectReason = reason.trim();
        this.closedAt = at;
    }

    /** 출고. 호출부가 같은 트랜잭션에서 보낼 매장 재고를 뺀다 */
    public void ship(Long actorStoreId, Instant at) {
        require(actorStoreId, TransferAction.SHIP);
        this.status = TransferStatus.SHIPPED;
        this.shippedAt = at;
    }

    /** 입고. 호출부가 같은 트랜잭션에서 받을 매장 재고를 더한다 */
    public void receive(Long actorStoreId, Instant at) {
        require(actorStoreId, TransferAction.RECEIVE);
        this.status = TransferStatus.RECEIVED;
        this.receivedAt = at;
    }

    public void cancel(Long actorStoreId, Instant at) {
        require(actorStoreId, TransferAction.CANCEL);
        this.status = TransferStatus.CANCELLED;
        this.closedAt = at;
    }

    public boolean involves(Long storeId) {
        return fromStoreId.equals(storeId) || toStoreId.equals(storeId);
    }

    public List<TransferLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /** 권한은 서비스가 이미 확인했다. 여기서는 매장 쪽(보내는/받는)과 상태만 본다 */
    private void require(Long actorStoreId, TransferAction action) {
        if (!availableActions(actorStoreId, true).contains(action)) {
            throw new IllegalTransferStateException(id, status, action);
        }
    }
}
