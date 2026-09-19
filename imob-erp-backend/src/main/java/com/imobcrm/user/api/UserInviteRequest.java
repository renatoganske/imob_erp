package com.imobcrm.user.api;

import com.imobcrm.user.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserInviteRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull Role role
) {
}
