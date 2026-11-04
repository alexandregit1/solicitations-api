package com.alexandre.solicitations.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
