package com.marinbom2.retailfnbplatform.inventory.api;

import com.marinbom2.retailfnbplatform.inventory.api.dto.RejectTransferRequest;
import com.marinbom2.retailfnbplatform.inventory.api.dto.TransferRequest;
import com.marinbom2.retailfnbplatform.inventory.api.dto.TransferResponse;
import com.marinbom2.retailfnbplatform.inventory.application.StockTransferService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매장 간 이동", description = "요청 → 승인 → 출고 → 입고 (SCR-08)")
@RestController
@RequestMapping("/api/v1/stores/{storeId}/transfers")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockTransferService transferService;

    @Operation(summary = "이동 요청", description = "FR-IN-003. 경로의 매장이 받는 매장이다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse request(@PathVariable Long storeId, @Valid @RequestBody TransferRequest request,
                                    @AuthenticationPrincipal Member member) {
        return transferService.request(storeId, request, member);
    }

    @Operation(summary = "이동 목록", description = "FR-IN-003. 보내고 받는 것 모두, 최신 50건")
    @GetMapping
    public List<TransferResponse> list(@PathVariable Long storeId, @AuthenticationPrincipal Member member) {
        return transferService.list(storeId, member);
    }

    @Operation(summary = "승인", description = "FR-IN-003. 보낼 매장")
    @ApiResponse(responseCode = "409", description = "TRANSFER_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{transferId}/approve")
    public TransferResponse approve(@PathVariable Long storeId, @PathVariable Long transferId,
                                    @AuthenticationPrincipal Member member) {
        return transferService.approve(storeId, transferId, member);
    }

    @Operation(summary = "거절", description = "FR-IN-003. 보낼 매장. 사유 필수")
    @PostMapping("/{transferId}/reject")
    public TransferResponse reject(@PathVariable Long storeId, @PathVariable Long transferId,
                                   @Valid @RequestBody RejectTransferRequest request, @AuthenticationPrincipal Member member) {
        return transferService.reject(storeId, transferId, request.reason(), member);
    }

    @Operation(summary = "출고", description = "FR-IN-003. 보낼 매장 재고가 빠진다")
    @ApiResponse(responseCode = "409", description = "OUT_OF_STOCK, TRANSFER_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{transferId}/ship")
    public TransferResponse ship(@PathVariable Long storeId, @PathVariable Long transferId,
                                 @AuthenticationPrincipal Member member) {
        return transferService.ship(storeId, transferId, member);
    }

    @Operation(summary = "입고", description = "FR-IN-003. 받을 매장 재고가 늘어난다")
    @PostMapping("/{transferId}/receive")
    public TransferResponse receive(@PathVariable Long storeId, @PathVariable Long transferId,
                                    @AuthenticationPrincipal Member member) {
        return transferService.receive(storeId, transferId, member);
    }

    @Operation(summary = "취소", description = "FR-IN-003. 받을 매장, 출고 전까지")
    @PostMapping("/{transferId}/cancel")
    public TransferResponse cancel(@PathVariable Long storeId, @PathVariable Long transferId,
                                   @AuthenticationPrincipal Member member) {
        return transferService.cancel(storeId, transferId, member);
    }
}
