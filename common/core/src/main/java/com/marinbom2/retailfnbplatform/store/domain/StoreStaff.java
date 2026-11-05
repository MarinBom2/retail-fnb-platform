package com.marinbom2.retailfnbplatform.store.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원이 어느 매장에서 어떤 역할로 일하는지. 한 회원이 여러 매장에 속할 수 있다 */
@Getter
@Entity
@Table(name = "store_staff",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_staff_member", columnNames = {"store_id", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreStaff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    public static StoreStaff assign(Long storeId, Long memberId, StaffRole role) {
        StoreStaff staff = new StoreStaff();
        staff.storeId = Objects.requireNonNull(storeId);
        staff.memberId = Objects.requireNonNull(memberId);
        staff.role = Objects.requireNonNull(role);
        return staff;
    }

    public void changeRole(StaffRole role) {
        this.role = Objects.requireNonNull(role);
    }
}
