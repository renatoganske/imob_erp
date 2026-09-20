package com.imobcrm.onboarding;

import com.imobcrm.config.OperatorProperties;
import com.imobcrm.shared.exception.ForbiddenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoint do OPERADOR do produto (nao dos usuarios das imobiliarias): protegido por chave de servico
 * no header X-Operator-Key, fora do fluxo de JWT. Sem OPERATOR_API_KEY configurada fica desligado.
 */
@RestController
@RequestMapping("/internal/v1/tenants")
@RequiredArgsConstructor
public class OperatorOnboardingController {

    private final OperatorProperties operator;
    private final OnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<TenantOnboardingResponse> create(
            @RequestHeader(value = "X-Operator-Key", required = false) String key,
            @Valid @RequestBody TenantOnboardingRequest request) {
        requireOperator(key);
        TenantOnboardingResponse response = onboardingService.onboard(request);
        return ResponseEntity.status(response.created() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    private void requireOperator(String key) {
        String expected = operator.apiKey();
        if (expected == null || expected.isBlank() || key == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Chave de operador invalida");
        }
    }
}
