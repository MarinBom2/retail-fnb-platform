package com.marinbom2.retailfnbplatform.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** method 는 PaymentMethod code (CARD, CASH, EASY_PAY). amount 는 합계와 같아야 한다 */
public record PaymentRequest(@NotBlank String method, @Min(0) long amount) {
}
