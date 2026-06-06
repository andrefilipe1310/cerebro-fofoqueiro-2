package com.fofoqueiro.alert.repository;

import com.fofoqueiro.alert.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM alerts.outbox_events WHERE sent_at IS NULL ORDER BY created_at LIMIT 50",
           nativeQuery = true)
    List<OutboxEvent> findUnsentEvents();
}
