package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.enums.BrazilianState;

public record CepAddress(String cep, String street, String neighborhood, String city, BrazilianState state) {}
