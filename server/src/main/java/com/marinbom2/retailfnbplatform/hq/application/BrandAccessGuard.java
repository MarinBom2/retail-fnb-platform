package com.marinbom2.retailfnbplatform.hq.application;

import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdminRepository;
import com.marinbom2.retailfnbplatform.store.domain.BrandRepository;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 본사 인가 (md/api.md §8). 그 브랜드의 관리자이거나 플랫폼 관리자만 통과한다 */
@Component
@RequiredArgsConstructor
public class BrandAccessGuard {

    private final BrandRepository brandRepository;
    private final BrandAdminRepository brandAdminRepository;

    /** @throws BusinessException NOT_FOUND_DATA(없는 브랜드), FORBIDDEN(관리자가 아님) */
    public Brand requireAdmin(Long brandId, Member member) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "브랜드를 찾을 수 없습니다."));
        if (!member.isAdmin() && !brandAdminRepository.existsByBrandIdAndMemberId(brandId, member.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이 브랜드를 관리할 권한이 없습니다.");
        }
        return brand;
    }
}
