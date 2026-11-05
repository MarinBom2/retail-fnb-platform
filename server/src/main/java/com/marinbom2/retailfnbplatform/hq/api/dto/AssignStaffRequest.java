package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** role 은 StaffRole code (OWNER, MANAGER, STAFF) */
public record AssignStaffRequest(@NotBlank @Email String email, @NotBlank String role) {
}
