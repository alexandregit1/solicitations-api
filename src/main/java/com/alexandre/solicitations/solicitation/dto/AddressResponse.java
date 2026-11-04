package com.alexandre.solicitations.solicitation.dto;

import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.solicitation.Address;

public record AddressResponse(
        String cep,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        BrazilianState state) {

    static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getCep(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState());
    }
}
