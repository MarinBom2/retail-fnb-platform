package com.marinbom2.retailfnbplatform.store.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.store.domain.exception.IllegalStoreStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoreTest {

    private final Brand giordano = DomainFixtures.brand(BusinessType.APPAREL);

    @Test
    @DisplayName("매장은 브랜드의 업종을 복사하고 오픈 준비 상태로 시작한다")
    void create() {
        Store store = Store.create(giordano, "GN-0012", "지오다노 명동점");

        assertThat(store.getBrandId()).isEqualTo(giordano.getId());
        assertThat(store.getBusinessType()).isEqualTo(BusinessType.APPAREL);
        assertThat(store.getStatus()).isEqualTo(StoreStatus.PREPARING);
        assertThat(store.isOperating()).isFalse();
    }

    @Test
    @DisplayName("PREPARING → OPEN → CLOSED → OPEN. 영업 중에 다시 open, 영업 전에 close 는 안 된다")
    void lifecycle() {
        Store store = Store.create(giordano, "GN-0012", "지오다노 명동점");

        assertThatThrownBy(store::close).isInstanceOf(IllegalStoreStateException.class);
        store.open();
        assertThatThrownBy(store::open).isInstanceOf(IllegalStoreStateException.class);
        store.close();
        assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
        store.open();
        assertThat(store.isOperating()).isTrue();
    }

    @Test
    @DisplayName("저장하지 않은 브랜드로는 매장을 만들 수 없다")
    void requiresSavedBrand() {
        Brand unsaved = Brand.register("새 브랜드", BusinessType.FOOD_SERVICE);

        assertThatThrownBy(() -> Store.create(unsaved, "A-1", "본점")).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("재고를 직접 바꾸는 일은 점주·매니저만")
    void staffRole() {
        assertThat(StaffRole.OWNER.canManageInventory()).isTrue();
        assertThat(StaffRole.MANAGER.canManageInventory()).isTrue();
        assertThat(StaffRole.STAFF.canManageInventory()).isFalse();
    }
}
