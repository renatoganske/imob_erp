package com.imobcrm.user.dto;

import com.imobcrm.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserInviteDTO(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull Role role
) {
}
