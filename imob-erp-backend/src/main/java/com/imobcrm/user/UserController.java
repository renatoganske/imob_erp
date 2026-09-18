package com.imobcrm.user;

import com.imobcrm.user.dto.UserCommissionRateDTO;
import com.imobcrm.user.dto.UserInviteDTO;
import com.imobcrm.user.dto.UserResponseDTO;
import com.imobcrm.user.dto.UserRoleUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> invite(@Valid @RequestBody UserInviteDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.invite(request));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO updateRole(@PathVariable UUID id, @Valid @RequestBody UserRoleUpdateDTO request) {
        return userService.updateRole(id, request.role());
    }

    @PatchMapping("/{id}/commission-rate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO updateCommissionRate(@PathVariable UUID id, @Valid @RequestBody UserCommissionRateDTO request) {
        return userService.updateCommissionRate(id, request.commissionRate());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
