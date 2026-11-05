package com.alexandre.solicitations.service;

import com.alexandre.solicitations.dto.CreateUserRequest;
import com.alexandre.solicitations.dto.RegisterRequest;
import com.alexandre.solicitations.dto.UserResponse;
import com.alexandre.solicitations.exception.BusinessException;
import com.alexandre.solicitations.exception.ConflictException;
import com.alexandre.solicitations.exception.NotFoundException;
import com.alexandre.solicitations.model.User;
import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.Role;
import com.alexandre.solicitations.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        return UserResponse.from(save(request.name(), request.email(), request.password(), Role.CLIENT, Set.of()));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (request.role() == Role.CLIENT) {
            throw new BusinessException("Clients sign up through /auth/register");
        }
        if (request.role() == Role.ANALYST && request.states().isEmpty()) {
            throw new BusinessException("An ANALYST needs at least one state");
        }
        if (request.role() == Role.ADMIN && !request.states().isEmpty()) {
            throw new BusinessException("Only an ANALYST has states");
        }
        return UserResponse.from(
                save(request.name(), request.email(), request.password(), request.role(), request.states()));
    }

    /** Empty when the email doesn't exist or the password doesn't match. */
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String email, String password) {
        return repository
                .findByEmail(normalize(email))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
    }

    @Transactional(readOnly = true)
    public Set<BrazilianState> getStates(UUID userId) {
        return repository
                .findById(userId)
                .map(user -> Set.copyOf(user.getStates()))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public void createAdminIfMissing(String email, String password) {
        if (!repository.existsByRole(Role.ADMIN)) {
            save("Administrator", email, password, Role.ADMIN, Set.of());
        }
    }

    private User save(String name, String email, String password, Role role, Set<BrazilianState> states) {
        String normalizedEmail = normalize(email);
        if (repository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already registered");
        }
        return repository.save(new User(name.strip(), normalizedEmail, passwordEncoder.encode(password), role, states));
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
