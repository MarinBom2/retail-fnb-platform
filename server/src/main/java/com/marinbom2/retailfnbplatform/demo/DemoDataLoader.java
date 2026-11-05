package com.marinbom2.retailfnbplatform.demo;

import com.marinbom2.retailfnbplatform.catalog.domain.ApparelOption;
import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductKind;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Recipe;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeLine;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.config.PlatformProperties;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovementRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockReference;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRequirement;
import com.marinbom2.retailfnbplatform.inventory.domain.StockTransfer;
import com.marinbom2.retailfnbplatform.inventory.domain.StockTransferRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferLine;
import com.marinbom2.retailfnbplatform.inventory.policy.StockDeductionPolicies;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.member.domain.MemberRepository;
import com.marinbom2.retailfnbplatform.order.domain.Order;
import com.marinbom2.retailfnbplatform.order.domain.OrderRepository;
import com.marinbom2.retailfnbplatform.order.domain.OrderType;
import com.marinbom2.retailfnbplatform.order.domain.PaymentMethod;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdmin;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdminRepository;
import com.marinbom2.retailfnbplatform.store.domain.BrandRepository;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.StaffRole;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaff;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaffRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * demo 프로필 데이터 (md/api.md §7). 가상의 세 브랜드(생활용품·의류·외식)와 매장 8곳, 오늘 매출, 안전재고 부족, 단계별 매장 간 이동.
 * 데모 계정은 세 브랜드의 관리자(본사)이자 모든 매장의 점주다.
 * 실제 상호를 쓰지 않는다. 모든 값은 도메인 메서드를 거쳐 만든다 (재고 원장과 매출이 서로 맞게).
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@retailfnb.dev";
    public static final String STAFF_EMAIL = "staff@retailfnb.dev";
    public static final String PASSWORD = "Demo1234!";

    private final MemberRepository memberRepository;
    private final BrandRepository brandRepository;
    private final BrandAdminRepository brandAdminRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final OrderRepository orderRepository;
    private final StockTransferRepository transferRepository;
    private final StockDeductionPolicies deductionPolicies;
    private final PasswordEncoder passwordEncoder;
    private final PlatformProperties platformProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private final Random random = new Random(42);
    private final Map<String, Stock> stocks = new HashMap<>();
    private long barcode = 8809990000010L;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        transactionTemplate.executeWithoutResult(tx -> seed());
        log.info("[demo] 데모 데이터를 만들었습니다. 로그인: {} / {}", DEMO_EMAIL, PASSWORD);
    }

    private void seed() {
        Member owner = memberRepository.save(Member.signUp(DEMO_EMAIL, passwordEncoder.encode(PASSWORD)));
        Member staff = memberRepository.save(Member.signUp(STAFF_EMAIL, passwordEncoder.encode(PASSWORD)));
        generalMerchandise(owner, staff);
        apparel(owner);
        foodService(owner);
    }

    // ---- 생활용품: 데일리마트 ----

    private void generalMerchandise(Member owner, Member staff) {
        Brand brand = brand("데일리마트", BusinessType.GENERAL_MERCHANDISE, owner);
        Store gangnam = store(brand, "DM-001", "강남점", owner);
        Store hongdae = store(brand, "DM-002", "홍대점", owner);
        Store pangyo = store(brand, "DM-003", "판교점", owner);
        storeStaffRepository.save(StoreStaff.assign(gangnam.getId(), staff.getId(), StaffRole.STAFF));

        Object[][] items = {
                {"스테인리스 머그컵 350ml", "주방", 3000}, {"실리콘 주걱", "주방", 2000}, {"밀폐용기 3종 세트", "주방", 5000},
                {"주방 스펀지 5입", "주방", 1000}, {"극세사 수건", "욕실", 2000}, {"규조토 발매트", "욕실", 5000},
                {"A5 스프링 노트", "문구", 1000}, {"젤펜 3색 세트", "문구", 2000}, {"알카라인 건전지 AA 4입", "생활", 2000},
                {"접이식 수납 바구니", "수납", 3000}, {"LED 무드등", "인테리어", 5000}, {"3단 자동 우산", "생활", 5000},
        };
        List<Sku> skus = new ArrayList<>();
        for (Object[] item : items) {
            Product product = Product.create(brand, ProductKind.MERCHANDISE, (String) item[0], (String) item[1]);
            product.addSku("DM-" + (skus.size() + 101), String.valueOf(barcode++), Money.won((Integer) item[2]), null, StockUnit.EA);
            productRepository.save(product);
            skus.add(product.getSkus().getFirst());
        }
        for (Store store : List.of(gangnam, hongdae, pangyo)) {
            skus.forEach(sku -> receive(store, sku, 60 + random.nextInt(60), 15));
        }

        sellToday(gangnam, OrderType.COUNTER, null, 28, () -> pick(skus, 1 + random.nextInt(3), 1, 3));
        sellToday(hongdae, OrderType.COUNTER, null, 18, () -> pick(skus, 1 + random.nextInt(3), 1, 2));
        sellToday(pangyo, OrderType.COUNTER, null, 12, () -> pick(skus, 1 + random.nextInt(2), 1, 2));

        // 강남점: 안전재고 부족 셋, 우산은 품절 (POS 에서 다른 매장 재고 찾기 시연)
        count(gangnam, skus.get(0), 4, 20);
        count(gangnam, skus.get(8), 6, 20);
        count(gangnam, skus.get(11), 0, 10);

        // 판교 → 강남 실리콘 주걱: 입고 완료 / 홍대 → 강남 수건: 출고됨(강남 입고 대기)
        // 강남 ← 홍대 건전지: 승인됨(홍대 출고 대기) / 판교가 강남에 노트 요청: 강남 승인 대기
        StockTransfer done = request(pangyo, gangnam, skus.get(1), 10, "주말 행사 대비", owner, 26);
        approve(done, 25);
        ship(done, 24);
        receive(done, 22);
        StockTransfer shipped = request(hongdae, gangnam, skus.get(4), 15, null, owner, 6);
        approve(shipped, 5);
        ship(shipped, 3);
        StockTransfer approved = request(hongdae, gangnam, skus.get(8), 30, "건전지 품절 임박", owner, 2);
        approve(approved, 1);
        request(gangnam, pangyo, skus.get(6), 20, "신학기 노트 수요", owner, 1);
    }

    // ---- 의류: 핏앤코 ----

    private void apparel(Member owner) {
        Brand brand = brand("핏앤코", BusinessType.APPAREL, owner);
        Store myeongdong = store(brand, "FC-001", "명동점", owner);
        Store seongsu = store(brand, "FC-002", "성수점", owner);
        Store jamsil = store(brand, "FC-003", "잠실점", owner);

        Product tee = style(brand, "크루넥 반팔 티셔츠", "상의", 19900, List.of("BLACK", "WHITE", "NAVY"), List.of("S", "M", "L", "XL"));
        Product hood = style(brand, "오버핏 후드 집업", "상의", 39900, List.of("GRAY", "BLACK"), List.of("M", "L", "XL"));
        Product denim = style(brand, "스트레치 슬림 데님", "하의", 49900, List.of("INDIGO", "BLACK"), List.of("28", "30", "32", "34"));
        Product vest = style(brand, "경량 패딩 조끼", "아우터", 59900, List.of("BLACK", "KHAKI"), List.of("M", "L", "XL"));
        List<Sku> all = new ArrayList<>();
        for (Product p : List.of(tee, hood, denim, vest)) {
            all.addAll(p.getSkus());
        }
        for (Store store : List.of(myeongdong, seongsu, jamsil)) {
            all.forEach(sku -> receive(store, sku, 6 + random.nextInt(10), 3));
        }

        sellToday(myeongdong, OrderType.COUNTER, null, 16, () -> pick(all, 1 + random.nextInt(2), 1, 1));
        sellToday(seongsu, OrderType.COUNTER, null, 11, () -> pick(all, 1 + random.nextInt(2), 1, 1));
        sellToday(jamsil, OrderType.COUNTER, null, 9, () -> pick(all, 1, 1, 1));

        Sku teeBlackL = find(tee, "BLACK", "L");
        Sku denimIndigo32 = find(denim, "INDIGO", "32");
        Sku vestKhakiL = find(vest, "KHAKI", "L");
        count(myeongdong, teeBlackL, 0, 3);
        count(myeongdong, denimIndigo32, 0, 3);
        count(myeongdong, find(hood, "GRAY", "M"), 2, 3);

        StockTransfer shipped = request(seongsu, myeongdong, teeBlackL, 5, "인기 사이즈 보충", owner, 4);
        approve(shipped, 3);
        ship(shipped, 2);
        request(myeongdong, jamsil, vestKhakiL, 2, "고객 예약 상품", owner, 1);
    }

    // ---- 외식: 온기식당 ----

    private void foodService(Member owner) {
        Brand brand = brand("온기식당", BusinessType.FOOD_SERVICE, owner);
        Store gwanghwamun = store(brand, "OG-001", "광화문점", owner);
        Store yeouido = store(brand, "OG-002", "여의도점", owner);

        Sku rice = ingredient(brand, "쌀", StockUnit.GRAM);
        Sku beef = ingredient(brand, "소고기 불고기용", StockUnit.GRAM);
        Sku pork = ingredient(brand, "돼지고기 앞다리", StockUnit.GRAM);
        Sku egg = ingredient(brand, "달걀", StockUnit.EA);
        Sku namul = ingredient(brand, "계절 나물", StockUnit.GRAM);
        Sku kimchi = ingredient(brand, "김치", StockUnit.GRAM);
        Sku tofu = ingredient(brand, "두부", StockUnit.GRAM);
        Sku scallion = ingredient(brand, "대파", StockUnit.GRAM);

        Sku bibimbap = menu(brand, "불고기 비빔밥", 11000, Map.of(rice, 210L, beef, 90L, namul, 70L, egg, 1L));
        Sku stew = menu(brand, "김치찌개", 9000, Map.of(kimchi, 150L, pork, 80L, tofu, 100L, scallion, 20L));
        Sku jeyuk = menu(brand, "제육볶음", 10000, Map.of(pork, 160L, scallion, 30L, rice, 210L));
        Sku omelet = menu(brand, "계란말이", 8000, Map.of(egg, 3L, scallion, 15L));
        Sku bowl = menu(brand, "공깃밥", 1000, Map.of(rice, 210L));
        List<Sku> menus = List.of(bibimbap, stew, jeyuk, omelet, bowl);

        Map<Sku, long[]> initial = new LinkedHashMap<>();
        initial.put(rice, new long[]{20000, 5000});
        initial.put(beef, new long[]{5000, 1500});
        initial.put(pork, new long[]{8000, 2000});
        initial.put(egg, new long[]{150, 40});
        initial.put(namul, new long[]{4000, 1000});
        initial.put(kimchi, new long[]{6000, 1500});
        initial.put(tofu, new long[]{4000, 1000});
        initial.put(scallion, new long[]{3000, 500});
        for (Store store : List.of(gwanghwamun, yeouido)) {
            initial.forEach((sku, v) -> receive(store, sku, v[0], v[1]));
        }

        sellToday(gwanghwamun, OrderType.DINE_IN, "T", 22, () -> pick(menus, 1 + random.nextInt(3), 1, 2));
        sellToday(yeouido, OrderType.TAKEOUT, null, 14, () -> pick(menus, 1 + random.nextInt(2), 1, 2));

        // 광화문점: 이용 중 테이블 둘, 달걀·소고기 안전재고 이하
        openTable(gwanghwamun, "3", Map.of(bibimbap, 2L, omelet, 1L));
        openTable(gwanghwamun, "7", Map.of(stew, 1L, bowl, 1L));
        count(gwanghwamun, egg, 28, 40);
        count(gwanghwamun, beef, 1200, 1500);

        request(yeouido, gwanghwamun, egg, 60, "점심 피크 대비", owner, 1);
    }

    // ---- 도우미 ----

    /** 데모 계정은 세 브랜드의 관리자(본사)이기도 하다 */
    private Brand brand(String name, BusinessType type, Member owner) {
        Brand brand = brandRepository.save(Brand.register(name, type));
        brandAdminRepository.save(BrandAdmin.of(brand, owner.getId()));
        return brand;
    }

    private Store store(Brand brand, String code, String name, Member owner) {
        Store store = storeRepository.save(Store.create(brand, code, name));
        store.open();
        storeStaffRepository.save(StoreStaff.assign(store.getId(), owner.getId(), StaffRole.OWNER));
        return store;
    }

    private Product style(Brand brand, String name, String category, long price, List<String> colors, List<String> sizes) {
        Product product = Product.create(brand, ProductKind.MERCHANDISE, name, category);
        String code = "FC-" + (100 + random.nextInt(900));
        for (String color : colors) {
            for (String size : sizes) {
                product.addSku(code + "-" + color + "-" + size, String.valueOf(barcode++), Money.won(price),
                        new ApparelOption(color, size), StockUnit.EA);
            }
        }
        return productRepository.save(product);
    }

    private Sku ingredient(Brand brand, String name, StockUnit unit) {
        Product product = Product.create(brand, ProductKind.INGREDIENT, name, "식자재");
        product.addSku("OG-I-" + barcode++, null, null, null, unit);
        return productRepository.save(product).getSkus().getFirst();
    }

    private Sku menu(Brand brand, String name, long price, Map<Sku, Long> recipe) {
        Product product = Product.create(brand, ProductKind.MENU, name, name.equals("공깃밥") ? "추가" : "식사");
        product.addSku("OG-M-" + barcode++, null, Money.won(price), null, StockUnit.EA);
        Sku sku = productRepository.save(product).getSkus().getFirst();
        recipeRepository.save(Recipe.define(sku, recipe.entrySet().stream()
                .map(e -> RecipeLine.of(e.getKey(), e.getValue())).toList()));
        return sku;
    }

    private static Sku find(Product style, String color, String size) {
        return style.getSkus().stream()
                .filter(s -> s.getOption().equals(new ApparelOption(color, size)))
                .findFirst().orElseThrow();
    }

    private Stock stock(Store store, Sku sku) {
        return stocks.computeIfAbsent(store.getId() + ":" + sku.getId(),
                key -> stockRepository.save(Stock.empty(store.getId(), sku.getId())));
    }

    private void receive(Store store, Sku sku, long quantity, long reorderPoint) {
        Stock stock = stock(store, sku);
        movementRepository.save(stock.receive(quantity, StockReference.manual()));
        stock.changeReorderPoint(reorderPoint);
    }

    /** 실사로 수량을 맞춘다 (부족·품절 상황 만들기) */
    private void count(Store store, Sku sku, long counted, long reorderPoint) {
        Stock stock = stock(store, sku);
        movementRepository.save(stock.adjustTo(counted, StockReference.manual()));
        stock.changeReorderPoint(reorderPoint);
    }

    private Map<Sku, Long> pick(List<Sku> skus, int kinds, int minQty, int maxQty) {
        Map<Sku, Long> picked = new LinkedHashMap<>();
        while (picked.size() < kinds) {
            picked.put(skus.get(random.nextInt(skus.size())), (long) (minQty + random.nextInt(maxQty - minQty + 1)));
        }
        return picked;
    }

    /** 오늘(영업일) 판매. 결제 시각을 오늘 안에 흩뿌리고, 업종별 정책으로 재고를 뺀다 */
    private void sellToday(Store store, OrderType type, String tablePrefix, int orders, java.util.function.Supplier<Map<Sku, Long>> basket) {
        ZoneId zone = platformProperties.zoneId();
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
        ZonedDateTime open = now.toLocalDate().atTime(10, 0).atZone(zone);
        ZonedDateTime from = now.isAfter(open.plusHours(1)) ? open : now.toLocalDate().atStartOfDay(zone);
        long span = Math.max(Duration.between(from, now).toSeconds() - 60, 1);
        PaymentMethod[] methods = {PaymentMethod.CARD, PaymentMethod.CARD, PaymentMethod.CARD, PaymentMethod.EASY_PAY, PaymentMethod.CASH};

        for (int i = 0; i < orders; i++) {
            Instant paidAt = from.plusSeconds((long) (random.nextDouble() * span)).toInstant();
            Order order = Order.open(store, type, tablePrefix == null ? null : tablePrefix + (1 + random.nextInt(12)));
            basket.get().forEach(order::addItem);
            orderRepository.save(order);
            order.pay(methods[random.nextInt(methods.length)], order.getTotalAmount(), paidAt);
            StockReference reference = StockReference.order(order.getId());
            order.getLines().forEach(line -> {
                for (StockRequirement r : deductionPolicies.of(store.getBusinessType()).requirementsFor(line.getSkuId(), line.getQuantity())) {
                    Stock stock = stocks.get(store.getId() + ":" + r.skuId());
                    movementRepository.save(stock.deductForSale(r.quantity(), reference));
                }
            });
        }
    }

    private void openTable(Store store, String table, Map<Sku, Long> items) {
        Order order = Order.open(store, OrderType.DINE_IN, table);
        items.forEach(order::addItem);
        orderRepository.save(order);
    }

    private StockTransfer request(Store from, Store to, Sku sku, long quantity, String memo, Member by, int hoursAgo) {
        return transferRepository.save(StockTransfer.request(from, to, List.of(TransferLine.of(sku, quantity)), memo, by.getId(), ago(hoursAgo)));
    }

    private void approve(StockTransfer t, int hoursAgo) {
        t.approve(t.getFromStoreId(), ago(hoursAgo));
    }

    private void ship(StockTransfer t, int hoursAgo) {
        t.ship(t.getFromStoreId(), ago(hoursAgo));
        t.getLines().forEach(line -> movementRepository.save(stocks.get(t.getFromStoreId() + ":" + line.skuId())
                .transferOut(line.quantity(), StockReference.transfer(t.getId()))));
    }

    private void receive(StockTransfer t, int hoursAgo) {
        t.receive(t.getToStoreId(), ago(hoursAgo));
        t.getLines().forEach(line -> movementRepository.save(stocks.get(t.getToStoreId() + ":" + line.skuId())
                .transferIn(line.quantity(), StockReference.transfer(t.getId()))));
    }

    private Instant ago(int hours) {
        return clock.instant().minus(Duration.ofHours(hours));
    }
}
