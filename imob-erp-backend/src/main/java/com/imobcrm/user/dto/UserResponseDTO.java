package com.imobcrm.user.dto;

import com.imobcrm.user.Role;

import java.math.BigDecimal;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        Role role,
        BigDecimal commissionRate,
        boolean active
) {
}
