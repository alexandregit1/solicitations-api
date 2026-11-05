package com.alexandre.solicitations.controller;

import com.alexandre.solicitations.dto.AddressRequest;
import com.alexandre.solicitations.dto.BasicInfoRequest;
import com.alexandre.solicitations.dto.ConfirmationRequest;
import com.alexandre.solicitations.dto.SolicitationResponse;
import com.alexandre.solicitations.security.AuthenticatedUser;
import com.alexandre.solicitations.service.SolicitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Client")
@RestController
@RequestMapping("/solicitations")
@RequiredArgsConstructor
public class SolicitationController {

    private final SolicitationService service;

    @Operation(summary = "Starts an empty DRAFT")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitationResponse create(@AuthenticationPrincipal AuthenticatedUser client) {
        return service.create(client.id());
    }

    @Operation(summary = "Returns one of the caller's solicitations, to resume a draft")
    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser client) {
        return service.get(id, client.id());
    }

    @Operation(summary = "Step 1: basic info")
    @PutMapping("/{id}/basic-info")
    public SolicitationResponse updateBasicInfo(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser client,
            @Valid @RequestBody BasicInfoRequest request) {
        return service.updateBasicInfo(id, client.id(), request);
    }

    @Operation(summary = "Step 2: address, completed from the CEP on ViaCEP")
    @PutMapping("/{id}/address")
    public SolicitationResponse updateAddress(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser client,
            @Valid @RequestBody AddressRequest request) {
        return service.updateAddress(id, client.id(), request);
    }

    @Operation(summary = "Step 3: confirmation")
    @PutMapping("/{id}/confirmation")
    public SolicitationResponse updateConfirmation(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser client,
            @Valid @RequestBody ConfirmationRequest request) {
        return service.updateConfirmation(id, client.id(), request);
    }

    @Operation(summary = "Validates every step and sends the DRAFT for review")
    @PostMapping("/{id}/submit")
    public SolicitationResponse submit(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser client) {
        return service.submit(id, client.id());
    }
}
