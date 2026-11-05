package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** An ANALYST or ADMIN. states: the UFs an ANALYST covers (required for ANALYST, empty for ADMIN). */
public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Set<@NotNull BrazilianState> states) {

    public CreateUserRequest {
        states = states == null ? Set.of() : states;
    }
}
