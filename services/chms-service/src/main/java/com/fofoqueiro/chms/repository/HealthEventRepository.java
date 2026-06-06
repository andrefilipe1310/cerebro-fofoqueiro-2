package com.fofoqueiro.chms.repository;

import com.fofoqueiro.chms.domain.entity.HealthEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HealthEventRepository extends JpaRepository<HealthEvent, UUID> {}
