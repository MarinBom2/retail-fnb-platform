package com.marinbom2.retailfnbplatform.hq.api;

import com.marinbom2.retailfnbplatform.hq.api.dto.BrandSummaryResponse;
import com.marinbom2.retailfnbplatform.hq.api.dto.CreateBrandRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.NameRequest;
import com.marinbom2.retailfnbplatform.hq.application.BrandService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "본사: 브랜드", description = "브랜드 목록·만들기 (SCR-10)")
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @Operation(summary = "내 브랜드", description = "FR-ST-001. 내가 관리하는 브랜드 (플랫폼 관리자는 전체)")
    @GetMapping
    public List<BrandSummaryResponse> myBrands(@AuthenticationPrincipal Member member) {
        return brandService.myBrands(member);
    }

    @Operation(summary = "브랜드 만들기", description = "FR-ST-001. 만든 사람이 브랜드 관리자가 된다. 업종은 바꿀 수 없다")
    @ApiResponse(responseCode = "409", description = "ALREADY_EXIST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandSummaryResponse create(@Valid @RequestBody CreateBrandRequest request, @AuthenticationPrincipal Member member) {
        return brandService.create(request.name(), request.businessType(), member);
    }

    @Operation(summary = "브랜드 요약", description = "FR-ST-001")
    @GetMapping("/{brandId}")
    public BrandSummaryResponse get(@PathVariable Long brandId, @AuthenticationPrincipal Member member) {
        return brandService.get(brandId, member);
    }

    @Operation(summary = "브랜드 이름 바꾸기", description = "FR-ST-001")
    @PatchMapping("/{brandId}")
    public BrandSummaryResponse rename(@PathVariable Long brandId, @Valid @RequestBody NameRequest request,
                                       @AuthenticationPrincipal Member member) {
        return brandService.rename(brandId, request.name(), member);
    }
}
