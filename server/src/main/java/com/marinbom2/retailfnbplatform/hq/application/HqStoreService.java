package com.marinbom2.retailfnbplatform.hq.application;

import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.hq.api.dto.HqStoreResponse;
import com.marinbom2.retailfnbplatform.hq.api.dto.StoreStaffResponse;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.member.domain.MemberRepository;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.StaffRole;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaff;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaffRepository;
import com.marinbom2.retailfnbplatform.type.common.EnumCodes;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-ST-002, FR-ST-003: 본사가 매장을 만들고 영업 상태와 직원을 관리한다 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HqStoreService {

    private final BrandAccessGuard brandAccessGuard;
    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<HqStoreResponse> list(Long brandId, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        List<Store> stores = storeRepository.findByBrandIdOrderByCodeAsc(brandId);
        Map<Long, Long> staffCounts = storeStaffRepository.findByStoreIdIn(stores.stream().map(Store::getId).toList()).stream()
                .collect(Collectors.groupingBy(StoreStaff::getStoreId, Collectors.counting()));
        return stores.stream().map(s -> HqStoreResponse.of(s, staffCounts.getOrDefault(s.getId(), 0L))).toList();
    }

    @Transactional
    public HqStoreResponse create(Long brandId, String code, String name, Member member) {
        Brand brand = brandAccessGuard.requireAdmin(brandId, member);
        String normalizedCode = code.trim().toUpperCase();
        if (storeRepository.existsByBrandIdAndCode(brandId, normalizedCode)) {
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 쓰는 매장 코드입니다.");
        }
        Store store = storeRepository.save(Store.create(brand, normalizedCode, name.trim()));
        log.info("[hq] store created storeId={} brandId={}", store.getId(), brandId);
        return HqStoreResponse.of(store, 0);
    }

    @Transactional
    public HqStoreResponse rename(Long brandId, Long storeId, String name, Member member) {
        Store store = storeOf(brandId, storeId, member);
        store.rename(name.trim());
        return response(store);
    }

    @Transactional
    public HqStoreResponse open(Long brandId, Long storeId, Member member) {
        Store store = storeOf(brandId, storeId, member);
        store.open();
        return response(store);
    }

    @Transactional
    public HqStoreResponse close(Long brandId, Long storeId, Member member) {
        Store store = storeOf(brandId, storeId, member);
        store.close();
        return response(store);
    }

    @Transactional(readOnly = true)
    public List<StoreStaffResponse> staff(Long brandId, Long storeId, Member member) {
        storeOf(brandId, storeId, member);
        List<StoreStaff> staff = storeStaffRepository.findByStoreIdOrderByIdAsc(storeId);
        Map<Long, String> emails = memberRepository.findAllById(staff.stream().map(StoreStaff::getMemberId).toList()).stream()
                .collect(Collectors.toMap(Member::getId, Member::getEmail));
        return staff.stream().map(s -> StoreStaffResponse.of(s, emails.get(s.getMemberId()))).toList();
    }

    /** 가입한 회원만 배정한다. 초대 메일은 아직 없다 (md/domain-design.md §14) */
    @Transactional
    public StoreStaffResponse assign(Long brandId, Long storeId, String email, String role, Member member) {
        storeOf(brandId, storeId, member);
        Member target = memberRepository.findByEmail(Member.normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER, "가입한 회원만 배정할 수 있습니다. 먼저 가입을 안내해 주세요."));
        if (storeStaffRepository.findByStoreIdAndMemberId(storeId, target.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 이 매장에 배정된 회원입니다.");
        }
        StoreStaff staff = storeStaffRepository.save(StoreStaff.assign(storeId, target.getId(), EnumCodes.parse(StaffRole.class, role)));
        log.info("[hq] staff assigned storeId={} memberId={}", storeId, target.getId());
        return StoreStaffResponse.of(staff, target.getEmail());
    }

    @Transactional
    public StoreStaffResponse changeRole(Long brandId, Long storeId, Long staffId, String role, Member member) {
        storeOf(brandId, storeId, member);
        StoreStaff staff = staffOf(storeId, staffId);
        staff.changeRole(EnumCodes.parse(StaffRole.class, role));
        return StoreStaffResponse.of(staff, memberRepository.findById(staff.getMemberId()).map(Member::getEmail).orElse(null));
    }

    @Transactional
    public void remove(Long brandId, Long storeId, Long staffId, Member member) {
        storeOf(brandId, storeId, member);
        storeStaffRepository.delete(staffOf(storeId, staffId));
        log.info("[hq] staff removed storeId={} staffId={}", storeId, staffId);
    }

    private Store storeOf(Long brandId, Long storeId, Member member) {
        brandAccessGuard.requireAdmin(brandId, member);
        return storeRepository.findById(storeId)
                .filter(store -> store.getBrandId().equals(brandId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "매장을 찾을 수 없습니다."));
    }

    private StoreStaff staffOf(Long storeId, Long staffId) {
        return storeStaffRepository.findById(staffId)
                .filter(staff -> staff.getStoreId().equals(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "직원을 찾을 수 없습니다."));
    }

    private HqStoreResponse response(Store store) {
        return HqStoreResponse.of(store, storeStaffRepository.findByStoreIdOrderByIdAsc(store.getId()).size());
    }
}
