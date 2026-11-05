package com.marinbom2.retailfnbplatform.hq.application;

import com.marinbom2.retailfnbplatform.catalog.application.CatalogLookup;
import com.marinbom2.retailfnbplatform.catalog.domain.ApparelOption;
import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductKind;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Recipe;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeLine;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.SkuRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.hq.api.dto.CreateProductRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.HqProductResponse;
import com.marinbom2.retailfnbplatform.hq.api.dto.NewSkuRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.RecipeRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.RecipeResponse;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.type.common.EnumCodes;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-CA-001~004: 본사 상품 등록·판매 상태·가격·레시피.
 * 업종별 규칙(상품 종류, 의류 색상×사이즈, 단일 SKU)은 Product 가 지키고, 여기서는 브랜드 단위 중복(SKU 코드·바코드)과 입력 변환만 맡는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HqProductService {

    private final BrandAccessGuard brandAccessGuard;
    private final CatalogLookup catalogLookup;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public List<HqProductResponse> list(Long brandId, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        List<Product> products = productRepository.findByBrandIdOrderByCategoryAscNameAsc(brandId);
        Set<Long> withRecipe = skusWithRecipe(products);
        return products.stream().map(p -> HqProductResponse.of(p, withRecipe)).toList();
    }

    @Transactional
    public HqProductResponse create(Long brandId, CreateProductRequest request, Member member) {
        Brand brand = brandAccessGuard.requireAdmin(brandId, member);
        requireUniqueCodes(brandId, request.skus());
        Product product = Product.create(brand, EnumCodes.parse(ProductKind.class, request.kind()), request.name().trim(),
                blankToNull(request.category()));
        request.skus().forEach(sku -> addSku(product, sku));
        productRepository.save(product);
        log.info("[hq] product created productId={} brandId={} skus={}", product.getId(), brandId, product.getSkus().size());
        return HqProductResponse.of(product, Set.of());
    }

    /** 의류 스타일에 색상·사이즈를 더한다 */
    @Transactional
    public HqProductResponse addSku(Long brandId, Long productId, NewSkuRequest request, Member member) {
        Product product = productOf(brandId, productId, member);
        requireUniqueCodes(brandId, List.of(request));
        addSku(product, request);
        productRepository.flush();
        return HqProductResponse.of(product, skusWithRecipe(List.of(product)));
    }

    @Transactional
    public HqProductResponse suspend(Long brandId, Long productId, Member member) {
        return changeStatus(brandId, productId, member, Product::suspend);
    }

    @Transactional
    public HqProductResponse resume(Long brandId, Long productId, Member member) {
        return changeStatus(brandId, productId, member, Product::resume);
    }

    @Transactional
    public HqProductResponse discontinue(Long brandId, Long productId, Member member) {
        return changeStatus(brandId, productId, member, Product::discontinue);
    }

    /** 브랜드 단일가 (md/domain-design.md §14 결정). 이미 담긴 주문의 단가는 바뀌지 않는다 */
    @Transactional
    public HqProductResponse.HqSku changePrice(Long brandId, Long skuId, long price, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        Sku sku = catalogLookup.skuOfBrand(brandId, skuId);
        sku.changePrice(Money.won(price));
        log.info("[hq] price changed skuId={}", skuId);
        return HqProductResponse.HqSku.of(sku, recipeRepository.findByMenuSkuId(skuId).isPresent());
    }

    @Transactional(readOnly = true)
    public RecipeResponse recipe(Long brandId, Long skuId, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        catalogLookup.skuOfBrand(brandId, skuId);
        return recipeRepository.findByMenuSkuId(skuId)
                .map(recipe -> recipeResponse(brandId, recipe))
                .orElseGet(() -> new RecipeResponse(skuId, List.of()));
    }

    /** 통째로 바꾼다. 이미 결제된 주문의 차감량은 바뀌지 않는다 */
    @Transactional
    public RecipeResponse replaceRecipe(Long brandId, Long skuId, RecipeRequest request, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        Sku menu = catalogLookup.skuOfBrand(brandId, skuId);
        Map<Long, Sku> ingredients = catalogLookup.skusOfBrand(brandId,
                        request.lines().stream().map(RecipeRequest.Line::ingredientSkuId).distinct().toList())
                .stream().collect(Collectors.toMap(Sku::getId, Function.identity()));
        List<RecipeLine> lines = request.lines().stream()
                .map(line -> RecipeLine.of(ingredients.get(line.ingredientSkuId()), line.quantity()))
                .toList();

        Recipe recipe = recipeRepository.findByMenuSkuId(skuId)
                .map(existing -> {
                    existing.replaceLines(lines);
                    return existing;
                })
                .orElseGet(() -> recipeRepository.save(Recipe.define(menu, lines)));
        log.info("[hq] recipe saved menuSkuId={} lines={}", skuId, lines.size());
        return recipeResponse(brandId, recipe);
    }

    private void addSku(Product product, NewSkuRequest request) {
        boolean hasColor = request.color() != null && !request.color().isBlank();
        boolean hasSize = request.size() != null && !request.size().isBlank();
        ApparelOption option = hasColor || hasSize ? new ApparelOption(request.color(), request.size()) : null;
        if (product.getKind().canBeSold() && request.price() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA, "판매 가격을 입력해 주세요.");
        }
        product.addSku(request.skuCode().trim(), blankToNull(request.barcode()),
                request.price() == null ? null : Money.won(request.price()),
                option, EnumCodes.parse(StockUnit.class, request.unit()));
    }

    /** SKU 코드·바코드는 브랜드 안에서 하나. 요청 안의 중복도 같이 본다 (DB 유니크 제약에 걸려 500 이 나지 않게) */
    private void requireUniqueCodes(Long brandId, List<NewSkuRequest> skus) {
        Set<String> codes = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        for (NewSkuRequest sku : skus) {
            String code = sku.skuCode().trim();
            if (!codes.add(code) || skuRepository.existsByBrandIdAndSkuCode(brandId, code)) {
                throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 쓰는 SKU 코드입니다: " + code);
            }
            String barcode = blankToNull(sku.barcode());
            if (barcode != null && (!barcodes.add(barcode) || skuRepository.existsByBrandIdAndBarcode(brandId, barcode))) {
                throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 쓰는 바코드입니다: " + barcode);
            }
        }
    }

    private HqProductResponse changeStatus(Long brandId, Long productId, Member member, Consumer<Product> change) {
        Product product = productOf(brandId, productId, member);
        change.accept(product);
        log.info("[hq] product status productId={} status={}", productId, product.getStatus());
        return HqProductResponse.of(product, skusWithRecipe(List.of(product)));
    }

    private Product productOf(Long brandId, Long productId, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        return productRepository.findById(productId)
                .filter(product -> product.getBrandId().equals(brandId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "상품을 찾을 수 없습니다."));
    }

    private Set<Long> skusWithRecipe(List<Product> products) {
        List<Long> menuSkuIds = products.stream()
                .filter(p -> p.getKind() == ProductKind.MENU)
                .flatMap(p -> p.getSkus().stream())
                .map(Sku::getId)
                .toList();
        if (menuSkuIds.isEmpty()) {
            return Set.of();
        }
        return recipeRepository.findByMenuSkuIdIn(menuSkuIds).stream().map(Recipe::getMenuSkuId).collect(Collectors.toSet());
    }

    private RecipeResponse recipeResponse(Long brandId, Recipe recipe) {
        Map<Long, Sku> ingredients = catalogLookup.skusOfBrand(brandId,
                        recipe.getLines().stream().map(RecipeLine::ingredientSkuId).toList())
                .stream().collect(Collectors.toMap(Sku::getId, Function.identity()));
        return new RecipeResponse(recipe.getMenuSkuId(), recipe.getLines().stream()
                .map(line -> {
                    Sku sku = ingredients.get(line.ingredientSkuId());
                    return new RecipeResponse.Line(sku.getId(), sku.displayName(), sku.getUnit(), line.quantity());
                })
                .toList());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
