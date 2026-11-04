package com.alexandre.solicitations.solicitation;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitationRepository extends JpaRepository<Solicitation, UUID> {}
