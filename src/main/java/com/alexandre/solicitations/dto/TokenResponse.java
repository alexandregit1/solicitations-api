package com.alexandre.solicitations.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
