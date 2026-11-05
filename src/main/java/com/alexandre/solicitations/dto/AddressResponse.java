package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.Address;
import com.alexandre.solicitations.model.enums.BrazilianState;

public record AddressResponse(
        String cep,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        BrazilianState state) {

    public static AddressResponse from(Address address) {
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
