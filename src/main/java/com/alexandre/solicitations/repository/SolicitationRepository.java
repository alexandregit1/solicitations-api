package com.alexandre.solicitations.repository;

import com.alexandre.solicitations.model.Solicitation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitationRepository extends JpaRepository<Solicitation, UUID> {}
