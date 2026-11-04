package com.alexandre.solicitations.solicitation;

import com.alexandre.solicitations.shared.BrazilianState;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Street, neighborhood, city and state come from ViaCEP; number and complement from the client. */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    private String cep;
    private String street;
    private String neighborhood;
    private String city;

    @Enumerated(EnumType.STRING)
    private BrazilianState state;

    private String number;
    private String complement;
}
