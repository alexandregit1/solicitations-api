package com.alexandre.solicitations.security;

import com.alexandre.solicitations.model.enums.Role;
import java.util.UUID;

/** The caller, read from the access token. Controllers get it with @AuthenticationPrincipal. */
public record AuthenticatedUser(UUID id, Role role) {

    public boolean is(Role role) {
        return this.role == role;
    }
}
