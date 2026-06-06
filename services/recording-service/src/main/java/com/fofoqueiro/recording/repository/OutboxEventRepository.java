package com.fofoqueiro.recording.repository;

import com.fofoqueiro.recording.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM recordings.outbox_events WHERE sent_at IS NULL ORDER BY created_at LIMIT 50",
           nativeQuery = true)
    List<OutboxEvent> findUnsentEvents();
}
