package com.alexandre.solicitations.cep;

import com.alexandre.solicitations.shared.BrazilianState;

public record CepAddress(String cep, String street, String neighborhood, String city, BrazilianState state) {}
