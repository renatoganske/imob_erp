package com.imobcrm.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** O administrador precisa ja ter uma conta no Clerk com este e-mail verificado. */
public record TenantOnboardingRequest(
        @NotBlank String tenantName,
        @NotBlank @Email String adminEmail
) {
}
