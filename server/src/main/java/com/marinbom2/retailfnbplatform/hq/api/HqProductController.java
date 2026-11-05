package com.marinbom2.retailfnbplatform.hq.api;

import com.marinbom2.retailfnbplatform.hq.api.dto.CreateProductRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.HqProductResponse;
import com.marinbom2.retailfnbplatform.hq.api.dto.NewSkuRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.PriceRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.RecipeRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.RecipeResponse;
import com.marinbom2.retailfnbplatform.hq.application.HqProductService;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "본사: 상품", description = "상품·SKU 등록, 판매 상태, 가격, 레시피 (SCR-12)")
@RestController
@RequestMapping("/api/v1/brands/{brandId}")
@RequiredArgsConstructor
public class HqProductController {

    private final HqProductService hqProductService;

    @Operation(summary = "브랜드 상품", description = "FR-CA-001")
    @GetMapping("/products")
    public List<HqProductResponse> list(@PathVariable Long brandId, @AuthenticationPrincipal Member member) {
        return hqProductService.list(brandId, member);
    }

    @Operation(summary = "상품 등록", description = "FR-CA-001. 의류는 색상×사이즈 SKU 를 한 번에. 업종 규칙에 맞지 않으면 400")
    @ApiResponse(responseCode = "409", description = "ALREADY_EXIST (SKU 코드·바코드)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public HqProductResponse create(@PathVariable Long brandId, @Valid @RequestBody CreateProductRequest request,
                                    @AuthenticationPrincipal Member member) {
        return hqProductService.create(brandId, request, member);
    }

    @Operation(summary = "SKU 추가", description = "FR-CA-001. 의류 스타일에 색상·사이즈를 더한다")
    @PostMapping("/products/{productId}/skus")
    @ResponseStatus(HttpStatus.CREATED)
    public HqProductResponse addSku(@PathVariable Long brandId, @PathVariable Long productId,
                                    @Valid @RequestBody NewSkuRequest request, @AuthenticationPrincipal Member member) {
        return hqProductService.addSku(brandId, productId, request, member);
    }

    @Operation(summary = "판매 중지", description = "FR-CA-002. POS 에서 담을 수 없게 된다")
    @PostMapping("/products/{productId}/suspend")
    public HqProductResponse suspend(@PathVariable Long brandId, @PathVariable Long productId, @AuthenticationPrincipal Member member) {
        return hqProductService.suspend(brandId, productId, member);
    }

    @Operation(summary = "판매 재개", description = "FR-CA-002. 단종 상품은 되돌릴 수 없다")
    @PostMapping("/products/{productId}/resume")
    public HqProductResponse resume(@PathVariable Long brandId, @PathVariable Long productId, @AuthenticationPrincipal Member member) {
        return hqProductService.resume(brandId, productId, member);
    }

    @Operation(summary = "단종", description = "FR-CA-002. 되돌릴 수 없다")
    @PostMapping("/products/{productId}/discontinue")
    public HqProductResponse discontinue(@PathVariable Long brandId, @PathVariable Long productId,
                                         @AuthenticationPrincipal Member member) {
        return hqProductService.discontinue(brandId, productId, member);
    }

    @Operation(summary = "가격 바꾸기", description = "FR-CA-004. 브랜드 단일가. 식자재는 400")
    @PatchMapping("/skus/{skuId}/price")
    public HqProductResponse.HqSku changePrice(@PathVariable Long brandId, @PathVariable Long skuId,
                                               @Valid @RequestBody PriceRequest request, @AuthenticationPrincipal Member member) {
        return hqProductService.changePrice(brandId, skuId, request.price(), member);
    }

    @Operation(summary = "레시피", description = "FR-CA-003. 없으면 빈 줄")
    @GetMapping("/skus/{skuId}/recipe")
    public RecipeResponse recipe(@PathVariable Long brandId, @PathVariable Long skuId, @AuthenticationPrincipal Member member) {
        return hqProductService.recipe(brandId, skuId, member);
    }

    @Operation(summary = "레시피 저장", description = "FR-CA-003. 통째로 바꾼다. 메뉴 SKU 에만, 재료는 같은 브랜드 식자재")
    @PutMapping("/skus/{skuId}/recipe")
    public RecipeResponse replaceRecipe(@PathVariable Long brandId, @PathVariable Long skuId,
                                        @Valid @RequestBody RecipeRequest request, @AuthenticationPrincipal Member member) {
        return hqProductService.replaceRecipe(brandId, skuId, request, member);
    }
}
