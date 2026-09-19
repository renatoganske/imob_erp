package com.imobcrm.user.api;

import com.imobcrm.user.domain.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull Role role) {
}
