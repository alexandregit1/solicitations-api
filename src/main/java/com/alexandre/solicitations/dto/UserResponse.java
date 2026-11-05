package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.User;
import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id, String name, String email, Role role, Set<BrazilianState> states, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                Set.copyOf(user.getStates()),
                user.getCreatedAt());
    }
}
