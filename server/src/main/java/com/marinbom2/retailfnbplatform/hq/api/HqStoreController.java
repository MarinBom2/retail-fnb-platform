package com.marinbom2.retailfnbplatform.hq.api;

import com.marinbom2.retailfnbplatform.hq.api.dto.AssignStaffRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.ChangeRoleRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.CreateStoreRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.HqStoreResponse;
import com.marinbom2.retailfnbplatform.hq.api.dto.NameRequest;
import com.marinbom2.retailfnbplatform.hq.api.dto.StoreStaffResponse;
import com.marinbom2.retailfnbplatform.hq.application.HqStoreService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "본사: 매장", description = "매장 등록·영업 상태·직원 (SCR-11)")
@RestController
@RequestMapping("/api/v1/brands/{brandId}/stores")
@RequiredArgsConstructor
public class HqStoreController {

    private final HqStoreService hqStoreService;

    @Operation(summary = "브랜드 매장 목록", description = "FR-ST-002")
    @GetMapping
    public List<HqStoreResponse> list(@PathVariable Long brandId, @AuthenticationPrincipal Member member) {
        return hqStoreService.list(brandId, member);
    }

    @Operation(summary = "매장 추가", description = "FR-ST-002. 오픈 준비 상태로 만든다. 코드는 브랜드 안에서 하나")
    @ApiResponse(responseCode = "409", description = "ALREADY_EXIST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HqStoreResponse create(@PathVariable Long brandId, @Valid @RequestBody CreateStoreRequest request,
                                  @AuthenticationPrincipal Member member) {
        return hqStoreService.create(brandId, request.code(), request.name(), member);
    }

    @Operation(summary = "매장 이름 바꾸기", description = "FR-ST-002")
    @PatchMapping("/{storeId}")
    public HqStoreResponse rename(@PathVariable Long brandId, @PathVariable Long storeId,
                                  @Valid @RequestBody NameRequest request, @AuthenticationPrincipal Member member) {
        return hqStoreService.rename(brandId, storeId, request.name(), member);
    }

    @Operation(summary = "영업 시작", description = "FR-ST-002")
    @ApiResponse(responseCode = "409", description = "STORE_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{storeId}/open")
    public HqStoreResponse open(@PathVariable Long brandId, @PathVariable Long storeId, @AuthenticationPrincipal Member member) {
        return hqStoreService.open(brandId, storeId, member);
    }

    @Operation(summary = "영업 종료", description = "FR-ST-002. 종료한 매장은 주문을 받지 않는다")
    @PostMapping("/{storeId}/close")
    public HqStoreResponse close(@PathVariable Long brandId, @PathVariable Long storeId, @AuthenticationPrincipal Member member) {
        return hqStoreService.close(brandId, storeId, member);
    }

    @Operation(summary = "매장 직원", description = "FR-ST-003")
    @GetMapping("/{storeId}/staff")
    public List<StoreStaffResponse> staff(@PathVariable Long brandId, @PathVariable Long storeId,
                                         @AuthenticationPrincipal Member member) {
        return hqStoreService.staff(brandId, storeId, member);
    }

    @Operation(summary = "직원 배정", description = "FR-ST-003. 가입한 회원의 이메일로 배정한다")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_MEMBER",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{storeId}/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreStaffResponse assign(@PathVariable Long brandId, @PathVariable Long storeId,
                                     @Valid @RequestBody AssignStaffRequest request, @AuthenticationPrincipal Member member) {
        return hqStoreService.assign(brandId, storeId, request.email(), request.role(), member);
    }

    @Operation(summary = "직원 역할 바꾸기", description = "FR-ST-003")
    @PatchMapping("/{storeId}/staff/{staffId}")
    public StoreStaffResponse changeRole(@PathVariable Long brandId, @PathVariable Long storeId, @PathVariable Long staffId,
                                         @Valid @RequestBody ChangeRoleRequest request, @AuthenticationPrincipal Member member) {
        return hqStoreService.changeRole(brandId, storeId, staffId, request.role(), member);
    }

    @Operation(summary = "직원 배정 해제", description = "FR-ST-003")
    @DeleteMapping("/{storeId}/staff/{staffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long brandId, @PathVariable Long storeId, @PathVariable Long staffId,
                       @AuthenticationPrincipal Member member) {
        hqStoreService.remove(brandId, storeId, staffId, member);
    }
}
