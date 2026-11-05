package com.marinbom2.retailfnbplatform.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockTest {

    private final Stock stock = DomainFixtures.stock(1L, 10L);

    @Test
    @DisplayName("수량을 바꿀 때마다 변동 기록이 같이 나오고, 기록의 잔량은 바뀐 뒤 수량이다")
    void ledger() {
        StockMovement received = stock.receive(10, StockReference.manual());
        StockMovement sold = stock.deductForSale(3, StockReference.order(5L));

        assertThat(stock.getQuantity()).isEqualTo(7);
        assertThat(received.getType()).isEqualTo(MovementType.RECEIVE);
        assertThat(received.getDelta()).isEqualTo(10);
        assertThat(received.getBalanceAfter()).isEqualTo(10);
        assertThat(sold.getType()).isEqualTo(MovementType.SALE);
        assertThat(sold.getDelta()).isEqualTo(-3);
        assertThat(sold.getBalanceAfter()).isEqualTo(7);
        assertThat(sold.getReference()).isEqualTo(StockReference.order(5L));
        assertThat(sold.getStoreId()).isEqualTo(1L);
        assertThat(sold.getSkuId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("경계값: 남은 수량만큼은 뺄 수 있고(0 이 됨), 하나라도 더 빼면 예외이며 수량은 그대로다")
    void insufficientBoundary() {
        stock.receive(5, StockReference.manual());

        assertThatThrownBy(() -> stock.deductForSale(6, StockReference.order(1L)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stock.getQuantity()).isEqualTo(5);

        stock.deductForSale(5, StockReference.order(1L));
        assertThat(stock.getQuantity()).isZero();
    }

    @Test
    @DisplayName("변동 수량은 1 이상이어야 한다 (0, 음수 거부)")
    void positiveAmount() {
        assertThatThrownBy(() -> stock.receive(0, StockReference.manual())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.deductForSale(-1, StockReference.order(1L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실사: 센 수량으로 맞추고 차이를 기록한다. 차이가 없어도 기록은 남는다")
    void adjust() {
        stock.receive(10, StockReference.manual());

        StockMovement lost = stock.adjustTo(8, StockReference.manual());
        StockMovement same = stock.adjustTo(8, StockReference.manual());

        assertThat(lost.getDelta()).isEqualTo(-2);
        assertThat(lost.getBalanceAfter()).isEqualTo(8);
        assertThat(same.getDelta()).isZero();
        assertThatThrownBy(() -> stock.adjustTo(-1, StockReference.manual())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매장 간 이동(의류): 보내는 쪽은 모자라면 못 보내고, 받는 쪽은 늘어난다")
    void transfer() {
        Stock to = DomainFixtures.stock(2L, 10L);
        stock.receive(3, StockReference.manual());

        assertThatThrownBy(() -> stock.transferOut(4, StockReference.transfer(9L)))
                .isInstanceOf(InsufficientStockException.class);
        StockMovement out = stock.transferOut(2, StockReference.transfer(9L));
        StockMovement in = to.transferIn(2, StockReference.transfer(9L));

        assertThat(out.getType()).isEqualTo(MovementType.TRANSFER_OUT);
        assertThat(in.getType()).isEqualTo(MovementType.TRANSFER_IN);
        assertThat(stock.getQuantity()).isEqualTo(1);
        assertThat(to.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("저장 전 재고(ID 없음)는 기록을 만들 수 없다: 서비스가 Stock 을 먼저 저장한다")
    void requiresSavedStock() {
        Stock unsaved = Stock.empty(1L, 10L);

        assertThatThrownBy(() -> unsaved.receive(1, StockReference.manual())).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("안전재고 경계값: 수량 == 안전재고면 보충 대상, +1 이면 아니다. 0 은 미설정")
    void reorderPoint() {
        stock.receive(10, StockReference.manual());
        assertThat(stock.needsReorder()).isFalse();

        stock.changeReorderPoint(10);
        assertThat(stock.needsReorder()).isTrue();
        stock.changeReorderPoint(9);
        assertThat(stock.needsReorder()).isFalse();
        assertThatThrownBy(() -> stock.changeReorderPoint(-1)).isInstanceOf(IllegalArgumentException.class);

        stock.deductForSale(10, StockReference.order(1L));
        stock.changeReorderPoint(0);
        assertThat(stock.needsReorder()).isFalse();
    }
}
