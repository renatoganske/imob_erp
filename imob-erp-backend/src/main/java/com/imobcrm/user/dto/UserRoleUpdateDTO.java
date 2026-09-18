package com.imobcrm.user.dto;

import com.imobcrm.user.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateDTO(@NotNull Role role) {
}
