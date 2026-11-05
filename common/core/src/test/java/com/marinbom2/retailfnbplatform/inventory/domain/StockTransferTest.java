package com.marinbom2.retailfnbplatform.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.IllegalTransferStateException;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InvalidTransferException;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockTransferTest {

    private static final Instant T0 = Instant.parse("2026-11-05T01:00:00Z");

    private final Brand brand = DomainFixtures.brand(BusinessType.APPAREL);
    private final Store myeongdong = DomainFixtures.openStore(brand);
    private final Store seongsu = DomainFixtures.openStore(brand);
    private final Product tee = DomainFixtures.apparelStyle(brand, "티셔츠");
    private final Sku blackL = DomainFixtures.apparelSku(tee, "BLACK", "L", 19_900);
    private final Sku blackM = DomainFixtures.apparelSku(tee, "BLACK", "M", 19_900);

    /** 명동(받을 매장)이 성수(보낼 매장)에 요청 */
    private StockTransfer requested() {
        return StockTransfer.request(seongsu, myeongdong, List.of(TransferLine.of(blackL, 5), TransferLine.of(blackM, 2)),
                " 인기 사이즈 ", 1L, T0);
    }

    @Test
    @DisplayName("요청 → 승인 → 출고 → 입고, 단계마다 시각이 남는다")
    void happyPath() {
        StockTransfer t = requested();
        assertThat(t.getStatus()).isEqualTo(TransferStatus.REQUESTED);
        assertThat(t.getMemo()).isEqualTo("인기 사이즈");
        assertThat(t.getLines()).extracting(TransferLine::itemName).containsExactly("티셔츠 (BLACK / L)", "티셔츠 (BLACK / M)");

        t.approve(seongsu.getId(), T0.plusSeconds(60));
        t.ship(seongsu.getId(), T0.plusSeconds(120));
        t.receive(myeongdong.getId(), T0.plusSeconds(180));

        assertThat(t.getStatus()).isEqualTo(TransferStatus.RECEIVED);
        assertThat(t.getApprovedAt()).isEqualTo(T0.plusSeconds(60));
        assertThat(t.getShippedAt()).isEqualTo(T0.plusSeconds(120));
        assertThat(t.getReceivedAt()).isEqualTo(T0.plusSeconds(180));
    }

    @Test
    @DisplayName("동작은 맞는 매장 쪽에서만: 받을 매장은 승인·출고 못 하고, 보낸 매장은 입고·취소 못 한다")
    void sideRules() {
        StockTransfer t = requested();

        assertThatThrownBy(() -> t.approve(myeongdong.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);
        t.approve(seongsu.getId(), T0);
        assertThatThrownBy(() -> t.ship(myeongdong.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);
        assertThatThrownBy(() -> t.cancel(seongsu.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);
        t.ship(seongsu.getId(), T0);
        assertThatThrownBy(() -> t.receive(seongsu.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);
    }

    @Test
    @DisplayName("상태 규칙: 승인 전 출고 불가, 출고 뒤 취소 불가, 거절은 사유 필수이고 끝난 이동은 더 못 바꾼다")
    void stateRules() {
        StockTransfer t = requested();
        assertThatThrownBy(() -> t.ship(seongsu.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);
        assertThatThrownBy(() -> t.reject(seongsu.getId(), " ", T0)).isInstanceOf(InvalidTransferException.class);

        t.reject(seongsu.getId(), "재고 부족", T0);
        assertThat(t.getStatus()).isEqualTo(TransferStatus.REJECTED);
        assertThat(t.getClosedAt()).isEqualTo(T0);
        assertThatThrownBy(() -> t.approve(seongsu.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);

        StockTransfer shipped = requested();
        shipped.approve(seongsu.getId(), T0);
        shipped.ship(seongsu.getId(), T0);
        assertThatThrownBy(() -> shipped.cancel(myeongdong.getId(), T0)).isInstanceOf(IllegalTransferStateException.class);
    }

    @Test
    @DisplayName("가능한 동작: 매장 쪽·상태·재고 권한으로 정해지고, 권한이 없으면 아무것도 없다")
    void availableActions() {
        StockTransfer t = requested();
        assertThat(t.availableActions(seongsu.getId(), true)).containsExactlyInAnyOrder(TransferAction.APPROVE, TransferAction.REJECT);
        assertThat(t.availableActions(myeongdong.getId(), true)).containsExactly(TransferAction.CANCEL);
        assertThat(t.availableActions(seongsu.getId(), false)).isEmpty();

        t.approve(seongsu.getId(), T0);
        assertThat(t.availableActions(seongsu.getId(), true)).containsExactly(TransferAction.SHIP);
        assertThat(t.availableActions(myeongdong.getId(), true)).containsExactly(TransferAction.CANCEL);
        t.ship(seongsu.getId(), T0);
        assertThat(t.availableActions(seongsu.getId(), true)).isEmpty();
        assertThat(t.availableActions(myeongdong.getId(), true)).containsExactly(TransferAction.RECEIVE);
    }

    @Test
    @DisplayName("요청 규칙: 다른 브랜드·같은 매장·빈 줄·같은 SKU 두 번·0 개·메뉴(재고 없음) 거부")
    void requestRules() {
        Brand other = DomainFixtures.brand(BusinessType.APPAREL);
        Store otherStore = DomainFixtures.openStore(other);
        List<TransferLine> one = List.of(TransferLine.of(blackL, 1));

        assertThatThrownBy(() -> StockTransfer.request(otherStore, myeongdong, one, null, 1L, T0)).isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> StockTransfer.request(myeongdong, myeongdong, one, null, 1L, T0)).isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> StockTransfer.request(seongsu, myeongdong, List.of(), null, 1L, T0)).isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> StockTransfer.request(seongsu, myeongdong,
                List.of(TransferLine.of(blackL, 1), TransferLine.of(blackL, 2)), null, 1L, T0)).isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> TransferLine.of(blackL, 0)).isInstanceOf(InvalidTransferException.class);

        Brand food = DomainFixtures.brand(BusinessType.FOOD_SERVICE);
        assertThatThrownBy(() -> TransferLine.of(DomainFixtures.menuSku(food, "김밥", 4000), 1)).isInstanceOf(InvalidTransferException.class);
    }
}
