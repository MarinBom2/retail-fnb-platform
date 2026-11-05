package com.marinbom2.retailfnbplatform.hq.application;

import com.marinbom2.retailfnbplatform.catalog.domain.ProductRepository;
import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.hq.api.dto.BrandSummaryResponse;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdmin;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdminRepository;
import com.marinbom2.retailfnbplatform.store.domain.BrandRepository;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.type.common.EnumCodes;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-ST-001: 브랜드 만들기·조회·이름 변경. 만든 사람이 그 브랜드의 관리자가 된다 (셀프 온보딩) */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandAccessGuard brandAccessGuard;
    private final BrandRepository brandRepository;
    private final BrandAdminRepository brandAdminRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<BrandSummaryResponse> myBrands(Member member) {
        List<Brand> brands = member.isAdmin()
                ? brandRepository.findAllByOrderByNameAsc()
                : brandRepository.findAllById(brandAdminRepository.findByMemberId(member.getId()).stream()
                        .map(BrandAdmin::getBrandId).toList());
        return brands.stream()
                .sorted(Comparator.comparing(Brand::getName))
                .map(this::summary)
                .toList();
    }

    @Transactional
    public BrandSummaryResponse create(String name, String businessType, Member member) {
        String trimmed = name.trim();
        if (brandRepository.existsByName(trimmed)) {
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 있는 브랜드 이름입니다.");
        }
        Brand brand = brandRepository.save(Brand.register(trimmed, EnumCodes.parse(BusinessType.class, businessType)));
        brandAdminRepository.save(BrandAdmin.of(brand, member.getId()));
        log.info("[hq] brand created brandId={} by memberId={}", brand.getId(), member.getId());
        return summary(brand);
    }

    @Transactional(readOnly = true)
    public BrandSummaryResponse get(Long brandId, Member member) {
        return summary(brandAccessGuard.requireAdmin(brandId, member));
    }

    @Transactional
    public BrandSummaryResponse rename(Long brandId, String name, Member member) {
        Brand brand = brandAccessGuard.requireAdmin(brandId, member);
        String trimmed = name.trim();
        if (!brand.getName().equals(trimmed) && brandRepository.existsByName(trimmed)) {
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 있는 브랜드 이름입니다.");
        }
        brand.rename(trimmed);
        return summary(brand);
    }

    private BrandSummaryResponse summary(Brand brand) {
        List<Store> stores = storeRepository.findByBrandIdOrderByCodeAsc(brand.getId());
        return BrandSummaryResponse.of(brand, stores.size(), stores.stream().filter(Store::isOperating).count(),
                productRepository.countByBrandId(brand.getId()));
    }
}
