package com.marinbom2.retailfnbplatform.hq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marinbom2.retailfnbplatform.demo.DemoDataLoader;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
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
 * 본사 셀프 온보딩을 실제 API 로 (md/api.md §8): 가입 → 브랜드 → 매장 → 직원 → 상품 → 매장에서 판매.
 * demo 프로필(H2)이라 Docker 없이 돈다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HqScenarioTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    @DisplayName("새 회원이 의류 브랜드를 만들고 매장·직원·색상×사이즈 상품을 등록하면, 매장 POS 카탈로그에 바로 나오고 판매된다")
    void apparelOnboarding() throws Exception {
        Cookie owner = signupAndLogin("hq-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com");
        Cookie clerkCookie = signupAndLogin("clerk-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com");
        String clerkEmail = json(get("/api/v1/me").cookie(clerkCookie)).get("email").asString();

        assertThat(json(get("/api/v1/brands").cookie(owner)).size()).isZero();
        JsonNode brand = json(post("/api/v1/brands").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"테스트의류\",\"businessType\":\"APPAREL\"}"), 201);
        long brandId = brand.get("brandId").asLong();
        assertThat(brand.get("businessType").get("code").asString()).isEqualTo("APPAREL");

        JsonNode store = json(post("/api/v1/brands/{b}/stores", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"tc-001\",\"name\":\"성수점\"}"), 201);
        long storeId = store.get("storeId").asLong();
        assertThat(store.get("code").asString()).isEqualTo("TC-001");
        assertThat(store.get("status").get("code").asString()).isEqualTo("PREPARING");
        perform(post("/api/v1/brands/{b}/stores", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TC-001\",\"name\":\"중복\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_EXIST"));
        json(post("/api/v1/brands/{b}/stores/{s}/open", brandId, storeId).cookie(owner));

        // 브랜드 관리자는 매장 운영 화면에도 본사 관리자로 들어간다
        JsonNode myStores = json(get("/api/v1/me/stores").cookie(owner));
        assertThat(myStores.get(0).get("role").get("code").asString()).isEqualTo("HQ_ADMIN");
        assertThat(myStores.get(0).get("canManageInventory").asBoolean()).isTrue();

        // 직원: 가입하지 않은 이메일 404, 배정 뒤 그 직원의 내 매장에 나온다
        perform(post("/api/v1/brands/{b}/stores/{s}/staff", brandId, storeId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"role\":\"STAFF\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND_MEMBER"));
        JsonNode staff = json(post("/api/v1/brands/{b}/stores/{s}/staff", brandId, storeId).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\",\"role\":\"STAFF\"}".formatted(clerkEmail)), 201);
        assertThat(json(get("/api/v1/me/stores").cookie(clerkCookie)).get(0).get("role").get("code").asString()).isEqualTo("STAFF");
        JsonNode promoted = json(patch("/api/v1/brands/{b}/stores/{s}/staff/{st}", brandId, storeId, staff.get("staffId").asLong())
                .cookie(owner).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MANAGER\"}"));
        assertThat(promoted.get("role").get("code").asString()).isEqualTo("MANAGER");

        // 상품: 색상 2 × 사이즈 2 = SKU 4개를 한 번에. 같은 SKU 코드는 409
        String skus = """
                [{"skuCode":"TEE-BK-M","price":19900,"color":"black","size":"m","unit":"EA"},
                 {"skuCode":"TEE-BK-L","price":19900,"color":"BLACK","size":"L","unit":"EA"},
                 {"skuCode":"TEE-WH-M","price":19900,"color":"WHITE","size":"M","unit":"EA","barcode":"8800000000001"},
                 {"skuCode":"TEE-WH-L","price":19900,"color":"WHITE","size":"L","unit":"EA"}]""";
        JsonNode product = json(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"MERCHANDISE\",\"name\":\"반팔 티셔츠\",\"category\":\"상의\",\"skus\":%s}".formatted(skus)), 201);
        assertThat(product.get("skus").size()).isEqualTo(4);
        assertThat(product.get("skus").get(0).get("displayName").asString()).isEqualTo("반팔 티셔츠 (BLACK / M)");
        perform(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"MERCHANDISE\",\"name\":\"다른 티\",\"skus\":[{\"skuCode\":\"TEE-BK-M\",\"price\":1000,\"color\":\"RED\",\"size\":\"S\",\"unit\":\"EA\"}]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_EXIST"));
        perform(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"MERCHANDISE\",\"name\":\"옵션 없는 옷\",\"skus\":[{\"skuCode\":\"NO-OPT\",\"price\":1000,\"unit\":\"EA\"}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));

        // 색상·사이즈 추가, 가격 변경
        long productId = product.get("productId").asLong();
        JsonNode added = json(post("/api/v1/brands/{b}/products/{p}/skus", brandId, productId).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"skuCode\":\"TEE-WH-XL\",\"price\":21900,\"color\":\"WHITE\",\"size\":\"XL\",\"unit\":\"EA\"}"), 201);
        assertThat(added.get("skus").size()).isEqualTo(5);
        long blackM = product.get("skus").get(0).get("skuId").asLong();
        assertThat(json(patch("/api/v1/brands/{b}/skus/{s}/price", brandId, blackM).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":17900}")).get("price").asLong()).isEqualTo(17900);

        // 매장에서: 카탈로그에 보이고, 입고 후 판매된다
        JsonNode catalog = json(get("/api/v1/stores/{s}/catalog", storeId).cookie(owner));
        assertThat(catalog.get(0).get("skus").size()).isEqualTo(5);
        json(post("/api/v1/stores/{s}/stocks/{sku}/receive", storeId, blackM).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"));
        JsonNode order = json(post("/api/v1/stores/{s}/orders", storeId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderType\":\"COUNTER\",\"items\":[{\"skuId\":%d,\"quantity\":1}]}".formatted(blackM)), 201);
        assertThat(order.get("totalAmount").asLong()).isEqualTo(17900);
        JsonNode paid = json(post("/api/v1/stores/{s}/orders/{o}/payment", storeId, order.get("orderId").asLong()).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CARD\",\"amount\":17900}"));
        assertThat(paid.get("status").get("code").asString()).isEqualTo("PAID");

        // 판매 중지하면 POS 에서 팔 수 없고, 단종은 되돌릴 수 없다
        json(post("/api/v1/brands/{b}/products/{p}/suspend", brandId, productId).cookie(owner));
        perform(post("/api/v1/stores/{s}/orders", storeId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderType\":\"COUNTER\",\"items\":[{\"skuId\":%d,\"quantity\":1}]}".formatted(blackM)))
                .andExpect(status().isBadRequest());
        json(post("/api/v1/brands/{b}/products/{p}/discontinue", brandId, productId).cookie(owner));
        perform(post("/api/v1/brands/{b}/products/{p}/resume", brandId, productId).cookie(owner))
                .andExpect(status().isBadRequest());

        // 직원 배정 해제
        perform(delete("/api/v1/brands/{b}/stores/{s}/staff/{st}", brandId, storeId, staff.get("staffId").asLong()).cookie(owner))
                .andExpect(status().isNoContent());
        assertThat(json(get("/api/v1/me/stores").cookie(clerkCookie)).size()).isZero();
    }

    @Test
    @DisplayName("외식: 식자재·메뉴 등록, 레시피 저장(바꾸기 포함), 식자재 가격 변경 400, 메뉴가 아닌 SKU 레시피 400")
    void foodServiceRecipe() throws Exception {
        Cookie owner = signupAndLogin("chef-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com");
        long brandId = json(post("/api/v1/brands").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"테스트식당\",\"businessType\":\"FOOD_SERVICE\"}"), 201).get("brandId").asLong();

        perform(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"MERCHANDISE\",\"name\":\"컵\",\"skus\":[{\"skuCode\":\"CUP\",\"price\":1000,\"unit\":\"EA\"}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
        long rice = json(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"INGREDIENT\",\"name\":\"쌀\",\"skus\":[{\"skuCode\":\"RICE\",\"unit\":\"GRAM\"}]}"), 201)
                .get("skus").get(0).get("skuId").asLong();
        long egg = json(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"INGREDIENT\",\"name\":\"달걀\",\"skus\":[{\"skuCode\":\"EGG\",\"unit\":\"EA\"}]}"), 201)
                .get("skus").get(0).get("skuId").asLong();
        perform(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"MENU\",\"name\":\"가격 없는 메뉴\",\"skus\":[{\"skuCode\":\"NOPRICE\",\"unit\":\"EA\"}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"));
        long omurice = json(post("/api/v1/brands/{b}/products", brandId).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"MENU\",\"name\":\"오므라이스\",\"category\":\"식사\",\"skus\":[{\"skuCode\":\"OMU\",\"price\":9000,\"unit\":\"EA\"}]}"), 201)
                .get("skus").get(0).get("skuId").asLong();

        assertThat(json(get("/api/v1/brands/{b}/skus/{s}/recipe", brandId, omurice).cookie(owner)).get("lines").size()).isZero();
        JsonNode recipe = json(put("/api/v1/brands/{b}/skus/{s}/recipe", brandId, omurice).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"ingredientSkuId\":%d,\"quantity\":210},{\"ingredientSkuId\":%d,\"quantity\":2}]}".formatted(rice, egg)));
        assertThat(recipe.get("lines").get(0).get("ingredientName").asString()).isEqualTo("쌀");
        assertThat(recipe.get("lines").get(0).get("unit").get("code").asString()).isEqualTo("GRAM");
        JsonNode replaced = json(put("/api/v1/brands/{b}/skus/{s}/recipe", brandId, omurice).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"ingredientSkuId\":%d,\"quantity\":3}]}".formatted(egg)));
        assertThat(replaced.get("lines").size()).isEqualTo(1);

        JsonNode products = json(get("/api/v1/brands/{b}/products", brandId).cookie(owner));
        boolean menuHasRecipe = false;
        for (JsonNode p : products) {
            if (p.get("name").asString().equals("오므라이스")) {
                menuHasRecipe = p.get("skus").get(0).get("hasRecipe").asBoolean();
            }
        }
        assertThat(menuHasRecipe).isTrue();

        perform(patch("/api/v1/brands/{b}/skus/{s}/price", brandId, rice).cookie(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":100}"))
                .andExpect(status().isBadRequest());
        perform(put("/api/v1/brands/{b}/skus/{s}/recipe", brandId, rice).cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"ingredientSkuId\":%d,\"quantity\":1}]}".formatted(egg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인가: 다른 브랜드 관리자·매장 직원은 본사 API 403, 다른 브랜드 SKU 는 404. 데모 계정은 세 브랜드 관리자")
    void authorization() throws Exception {
        Cookie demo = login(DemoDataLoader.DEMO_EMAIL, DemoDataLoader.PASSWORD);
        JsonNode demoBrands = json(get("/api/v1/brands").cookie(demo));
        assertThat(demoBrands.size()).isEqualTo(3);
        long demoBrandId = demoBrands.get(0).get("brandId").asLong();
        assertThat(demoBrands.get(0).get("storeCount").asLong()).isPositive();

        Cookie outsider = signupAndLogin("out-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com");
        perform(get("/api/v1/brands/{b}/stores", demoBrandId).cookie(outsider))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        Cookie staff = login(DemoDataLoader.STAFF_EMAIL, DemoDataLoader.PASSWORD);
        perform(get("/api/v1/brands/{b}/products", demoBrandId).cookie(staff)).andExpect(status().isForbidden());

        long ownBrand = json(post("/api/v1/brands").cookie(outsider).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"남의브랜드\",\"businessType\":\"GENERAL_MERCHANDISE\"}"), 201).get("brandId").asLong();
        long demoSku = json(get("/api/v1/brands/{b}/products", demoBrandId).cookie(demo)).get(0).get("skus").get(0).get("skuId").asLong();
        perform(patch("/api/v1/brands/{b}/skus/{s}/price", ownBrand, demoSku).cookie(outsider)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isNotFound());
        perform(post("/api/v1/brands").cookie(outsider).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"데일리마트\",\"businessType\":\"APPAREL\"}"))
                .andExpect(status().isConflict());
        perform(post("/api/v1/brands").cookie(outsider).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"이상한업종\",\"businessType\":\"CAFE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"));
    }

    private Cookie signupAndLogin(String email) throws Exception {
        perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    private Cookie login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("retailfnb_at");
    }

    private ResultActions perform(RequestBuilder request) throws Exception {
        return mockMvc.perform(request);
    }

    private JsonNode json(RequestBuilder request) throws Exception {
        return json(request, 200);
    }

    private JsonNode json(RequestBuilder request, int expected) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body);
    }
}
