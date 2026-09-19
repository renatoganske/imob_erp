package com.imobcrm.user.api;

import com.imobcrm.user.domain.enums.Role;
import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        BigDecimal commissionRate,
        boolean active
) {
}
