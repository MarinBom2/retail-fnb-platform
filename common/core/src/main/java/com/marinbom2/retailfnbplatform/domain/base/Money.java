package com.marinbom2.retailfnbplatform.domain.base;

import jakarta.persistence.Embeddable;

/**
 * 원화 금액 (원 단위 정수). 음수는 만들 수 없다. 할인·환불은 금액을 빼지 않고 상태로 표현한다.
 * 엔티티에 둘 이상 넣을 때는 @AttributeOverride 로 칼럼 이름을 나눈다.
 */
@Embeddable
public record Money(long amount) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다: " + amount);
        }
    }

    public static Money won(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(amount, other.amount));
    }

    public Money times(long quantity) {
        return new Money(Math.multiplyExact(amount, quantity));
    }
}
