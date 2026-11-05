package com.alexandre.solicitations.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;
    private UUID actorId;
    private UUID resourceId;
    private Instant createdAt;

    public AuditLog(String action, UUID actorId, UUID resourceId) {
        this.action = action;
        this.actorId = actorId;
        this.resourceId = resourceId;
        this.createdAt = Instant.now();
    }
}
