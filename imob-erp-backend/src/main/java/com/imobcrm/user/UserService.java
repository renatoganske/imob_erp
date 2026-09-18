package com.imobcrm.user;

import com.imobcrm.shared.exception.ResourceNotFoundException;
import com.imobcrm.tenant.TenantContext;
import com.imobcrm.user.dto.UserInviteDTO;
import com.imobcrm.user.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAllByTenantId(TenantContext.tenantId()).stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public UserResponseDTO invite(UserInviteDTO request) {
        // Em produção, isto dispararia um convite via Clerk (email) e o
        // registro local seria criado pelo webhook de user.created.
        User user = User.builder()
                .id(UUID.randomUUID())
                .tenantId(TenantContext.tenantId())
                .clerkUserId("pending:" + UUID.randomUUID())
                .name(request.name())
                .email(request.email())
                .role(request.role())
                .active(false)
                .build();
        User saved = userRepository.save(user);
        log.info("Usuario convidado: id={} email={} role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return userMapper.toResponseDTO(saved);
    }

    @Transactional
    public UserResponseDTO updateRole(UUID id, Role role) {
        User user = findOwned(id);
        Role previousRole = user.getRole();
        user.setRole(role);
        log.info("Papel do usuario {} alterado: {} -> {}", id, previousRole, role);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO updateCommissionRate(UUID id, BigDecimal commissionRate) {
        User user = findOwned(id);
        user.setCommissionRate(commissionRate);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = findOwned(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("Usuario {} desativado", id);
    }

    private User findOwned(UUID id) {
        return userRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
