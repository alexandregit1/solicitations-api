package com.alexandre.solicitations.user;

import com.alexandre.solicitations.shared.BrazilianState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String email;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    // UFs an ANALYST covers; empty for the other roles
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_states", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private Set<BrazilianState> states = new HashSet<>();

    private Instant createdAt;

    public User(String name, String email, String passwordHash, Role role, Set<BrazilianState> states) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.states.addAll(states);
        this.createdAt = Instant.now();
    }
}
