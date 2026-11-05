package com.marinbom2.retailfnbplatform.store.api;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.api.dto.MyStoreResponse;
import com.marinbom2.retailfnbplatform.store.application.MyStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매장", description = "매장 선택 (SCR-03)")
@RestController
@RequiredArgsConstructor
public class MyStoreController {

    private final MyStoreService myStoreService;

    @Operation(summary = "내 매장 목록", description = "FR-ST-003. 내가 직원으로 배정된 매장과 역할")
    @GetMapping("/api/v1/me/stores")
    public List<MyStoreResponse> myStores(@AuthenticationPrincipal Member member) {
        return myStoreService.myStores(member.getId());
    }
}
