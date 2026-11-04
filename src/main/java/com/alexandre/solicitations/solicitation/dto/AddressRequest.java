package com.alexandre.solicitations.solicitation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Step 2. The rest of the address comes from ViaCEP. */
public record AddressRequest(
        @NotNull @Pattern(regexp = "\\d{5}-?\\d{3}", message = "must be a CEP like 00000-000")
        String cep,

        @NotNull @Size(min = 1, max = 20) String number,
        @Size(max = 100) String complement) {

    public AddressRequest {
        cep = cep == null ? null : cep.strip();
        number = number == null ? null : number.strip();
        complement = complement == null || complement.isBlank() ? null : complement.strip();
    }

    public String cepDigits() {
        return cep.replace("-", "");
    }
}
