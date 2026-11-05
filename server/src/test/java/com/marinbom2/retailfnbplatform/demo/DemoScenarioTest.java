package com.marinbom2.retailfnbplatform.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * demo 프로필(H2 + 데모 데이터)로 실제 API 를 처음부터 끝까지 부른다 (md/api.md). Docker 없이 돈다.
 * 시나리오마다 데이터가 바뀌므로 테스트마다 컨텍스트를 새로 띄운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DemoScenarioTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    Cookie owner;

    @BeforeEach
    void login() throws Exception {
        owner = loginAs(DemoDataLoader.DEMO_EMAIL);
    }

    @Test
    @DisplayName("내 매장 8곳, 대시보드에 오늘 매출·시간대 24칸·안전재고 부족(추천 매장 포함)·처리할 이동")
    void storesAndDashboard() throws Exception {
        JsonNode stores = json(get("/api/v1/me/stores").cookie(owner));
        assertThat(stores.size()).isEqualTo(8);
        assertThat(stores.get(0).get("businessType").get("code").asString()).isNotBlank();

        long gangnam = storeId(stores, "데일리마트 강남점");
        JsonNode dashboard = json(get("/api/v1/stores/{id}/dashboard", gangnam).cookie(owner));
        assertThat(dashboard.get("salesAmount").asLong()).isPositive();
        assertThat(dashboard.get("orderCount").asLong()).isEqualTo(28);
        assertThat(dashboard.get("hourlySales").size()).isEqualTo(24);
        assertThat(dashboard.get("topItems").size()).isBetween(1, 5);
        assertThat(dashboard.get("lowStock").size()).isEqualTo(3);
        assertThat(dashboard.get("lowStock").get(0).get("bestSourceStoreName").asString()).endsWith("점");
        assertThat(dashboard.get("transfersToApprove").asLong()).isEqualTo(1);
        assertThat(dashboard.get("transfersToReceive").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("POS: 품절 상품 결제는 409 OUT_OF_STOCK + skuId, 다른 매장 재고로 이동 요청 → 승인 → 출고 → 입고 뒤 판매된다")
    void outOfStockToTransferToSale() throws Exception {
        JsonNode stores = json(get("/api/v1/me/stores").cookie(owner));
        long gangnam = storeId(stores, "데일리마트 강남점");
        long umbrella = skuIdByName(gangnam, "3단 자동 우산");

        JsonNode order = json(post("/api/v1/stores/{id}/orders", gangnam).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderType\":\"COUNTER\",\"items\":[{\"skuId\":%d,\"quantity\":1}]}".formatted(umbrella)), 201);
        long orderId = order.get("orderId").asLong();
        perform(post("/api/v1/stores/{id}/orders/{o}/payment", gangnam, orderId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"CARD\",\"amount\":5000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.skuId").value(umbrella));
        assertThat(json(get("/api/v1/stores/{id}/orders/{o}", gangnam, orderId).cookie(owner)).get("status").get("code").asString())
                .isEqualTo("OPEN");

        JsonNode availability = json(get("/api/v1/stores/{id}/stocks/{s}/availability", gangnam, umbrella).cookie(owner));
        long source = availability.get(0).get("storeId").asLong();

        JsonNode transfer = json(post("/api/v1/stores/{id}/transfers", gangnam).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromStoreId\":%d,\"lines\":[{\"skuId\":%d,\"quantity\":3}],\"memo\":\"품절\"}".formatted(source, umbrella)), 201);
        long transferId = transfer.get("transferId").asLong();
        assertThat(transfer.get("direction").asString()).isEqualTo("INCOMING");
        assertThat(transfer.get("availableActions").toString()).contains("CANCEL").doesNotContain("APPROVE");

        // 받을 매장은 승인할 수 없다
        perform(post("/api/v1/stores/{id}/transfers/{t}/approve", gangnam, transferId).cookie(owner))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRANSFER_STATE_CONFLICT"));

        long sourceBefore = quantity(source, umbrella);
        assertThat(json(post("/api/v1/stores/{id}/transfers/{t}/approve", source, transferId).cookie(owner))
                .get("availableActions").toString()).contains("SHIP");
        json(post("/api/v1/stores/{id}/transfers/{t}/ship", source, transferId).cookie(owner));
        assertThat(quantity(source, umbrella)).isEqualTo(sourceBefore - 3);
        JsonNode received = json(post("/api/v1/stores/{id}/transfers/{t}/receive", gangnam, transferId).cookie(owner));
        assertThat(received.get("status").get("code").asString()).isEqualTo("RECEIVED");
        assertThat(received.get("receivedAt").asString()).endsWith("Z");
        assertThat(quantity(gangnam, umbrella)).isEqualTo(3);

        JsonNode paid = json(post("/api/v1/stores/{id}/orders/{o}/payment", gangnam, orderId).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CARD\",\"amount\":5000}"));
        assertThat(paid.get("status").get("code").asString()).isEqualTo("PAID");
        assertThat(quantity(gangnam, umbrella)).isEqualTo(2);

        JsonNode movements = json(get("/api/v1/stores/{id}/stocks/{s}/movements", gangnam, umbrella).cookie(owner));
        assertThat(movements.get(0).get("type").get("code").asString()).isEqualTo("SALE");
        assertThat(movements.get(1).get("type").get("code").asString()).isEqualTo("TRANSFER_IN");
    }

    @Test
    @DisplayName("외식: 이용 중 테이블에 메뉴를 더하고 결제하면 레시피대로 식자재가 빠진다. 메뉴는 재고 목록에 없다")
    void foodServiceTable() throws Exception {
        JsonNode stores = json(get("/api/v1/me/stores").cookie(owner));
        long store = storeId(stores, "온기식당 광화문점");
        JsonNode open = json(get("/api/v1/stores/{id}/orders?status=OPEN", store).cookie(owner));
        assertThat(open.size()).isEqualTo(2);
        JsonNode table = open.get(0);
        long orderId = table.get("orderId").asLong();
        long omelet = skuIdByName(store, "계란말이");
        long egg = skuIdByName(store, "달걀");
        long eggBefore = quantity(store, egg);

        JsonNode added = json(post("/api/v1/stores/{id}/orders/{o}/items", store, orderId).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"skuId\":%d,\"quantity\":1}".formatted(omelet)));
        long total = added.get("totalAmount").asLong();
        long eggsInOrder = 0;
        for (JsonNode line : added.get("lines")) {
            String name = line.get("itemName").asString();
            long qty = line.get("quantity").asLong();
            eggsInOrder += name.equals("계란말이") ? 3 * qty : name.equals("불고기 비빔밥") ? qty : 0;
        }

        json(post("/api/v1/stores/{id}/orders/{o}/payment", store, orderId).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CARD\",\"amount\":%d}".formatted(total)));
        assertThat(quantity(store, egg)).isEqualTo(eggBefore - eggsInOrder);

        JsonNode stocks = json(get("/api/v1/stores/{id}/stocks", store).cookie(owner));
        for (JsonNode s : stocks) {
            assertThat(s.get("kind").get("code").asString()).isEqualTo("INGREDIENT");
        }
    }

    @Test
    @DisplayName("인가: 직원은 재고를 바꿀 수 없고(403), 배정되지 않은 매장은 볼 수 없다(403). 안전재고 설정은 점주")
    void authorization() throws Exception {
        Cookie staff = loginAs(DemoDataLoader.STAFF_EMAIL);
        JsonNode staffStores = json(get("/api/v1/me/stores").cookie(staff));
        assertThat(staffStores.size()).isEqualTo(1);
        assertThat(staffStores.get(0).get("canManageInventory").asBoolean()).isFalse();
        long gangnam = staffStores.get(0).get("storeId").asLong();
        long cup = skuIdByName(gangnam, "스테인리스 머그컵 350ml", staff);

        perform(post("/api/v1/stores/{id}/stocks/{s}/receive", gangnam, cup).cookie(staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":10}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        long other = storeId(json(get("/api/v1/me/stores").cookie(owner)), "핏앤코 명동점");
        perform(get("/api/v1/stores/{id}/dashboard", other).cookie(staff)).andExpect(status().isForbidden());
        perform(get("/api/v1/stores/{id}/transfers", gangnam).cookie(staff)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableActions").isEmpty());

        JsonNode updated = json(put("/api/v1/stores/{id}/stocks/{s}/reorder-point", gangnam, cup).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reorderPoint\":4}"));
        assertThat(updated.get("needsReorder").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("의류 카탈로그: 스타일마다 색상×사이즈 SKU 와 이 매장 재고, 명동점 BLACK/L 은 0")
    void apparelCatalog() throws Exception {
        long store = storeId(json(get("/api/v1/me/stores").cookie(owner)), "핏앤코 명동점");
        JsonNode catalog = json(get("/api/v1/stores/{id}/catalog", store).cookie(owner));
        JsonNode tee = null;
        for (JsonNode p : catalog) {
            if (p.get("name").asString().equals("크루넥 반팔 티셔츠")) {
                tee = p;
            }
        }
        assertThat(tee).isNotNull();
        assertThat(tee.get("skus").size()).isEqualTo(12);
        for (JsonNode sku : tee.get("skus")) {
            if (sku.get("color").asString().equals("BLACK") && sku.get("size").asString().equals("L")) {
                assertThat(sku.get("stockQuantity").asLong()).isZero();
                assertThat(sku.get("displayName").asString()).isEqualTo("크루넥 반팔 티셔츠 (BLACK / L)");
            }
        }
    }

    private Cookie loginAs(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DemoDataLoader.PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("retailfnb_at");
    }

    private long storeId(JsonNode stores, String name) {
        for (JsonNode s : stores) {
            if ((s.get("brandName").asString() + " " + s.get("storeName").asString()).equals(name)) {
                return s.get("storeId").asLong();
            }
        }
        throw new AssertionError("매장 없음: " + name);
    }

    private long skuIdByName(long storeId, String name) throws Exception {
        return skuIdByName(storeId, name, owner);
    }

    private long skuIdByName(long storeId, String name, Cookie cookie) throws Exception {
        for (JsonNode p : json(get("/api/v1/stores/{id}/catalog", storeId).cookie(cookie))) {
            for (JsonNode sku : p.get("skus")) {
                if (sku.get("displayName").asString().equals(name)) {
                    return sku.get("skuId").asLong();
                }
            }
        }
        throw new AssertionError("상품 없음: " + name);
    }

    private long quantity(long storeId, long skuId) throws Exception {
        for (JsonNode s : json(get("/api/v1/stores/{id}/stocks", storeId).cookie(owner))) {
            if (s.get("skuId").asLong() == skuId) {
                return s.get("quantity").asLong();
            }
        }
        throw new AssertionError("재고 없음: " + skuId);
    }

    private ResultActions perform(RequestBuilder request) throws Exception {
        return mockMvc.perform(request);
    }

    private JsonNode json(RequestBuilder request) throws Exception {
        return json(request, 200);
    }

    private JsonNode json(RequestBuilder request, int expectedStatus) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body);
    }
}
