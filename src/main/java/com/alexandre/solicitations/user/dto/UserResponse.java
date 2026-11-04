package com.alexandre.solicitations.user.dto;

import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.user.Role;
import com.alexandre.solicitations.user.User;
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
