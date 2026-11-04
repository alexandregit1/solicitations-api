package com.alexandre.solicitations.auth;

import com.alexandre.solicitations.user.Role;
import java.util.UUID;

/** The caller, read from the access token. Controllers get it with @AuthenticationPrincipal. */
public record AuthenticatedUser(UUID id, Role role) {

    public boolean is(Role role) {
        return this.role == role;
    }
}
