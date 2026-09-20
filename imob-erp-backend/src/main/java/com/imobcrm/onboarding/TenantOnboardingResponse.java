package com.imobcrm.onboarding;

import java.util.UUID;

public record TenantOnboardingResponse(
        UUID tenantId,
        String slug,
        UUID adminUserId,
        String adminClerkUserId,
        boolean created
) {
}
