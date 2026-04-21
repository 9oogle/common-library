package com.goggles.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
  List<Outbox> findByStatusInAndRetryCountLessThan(List<OutboxStatus> statuses, int limit);
  List<Outbox> findByStatusAndUpdatedAtBefore(OutboxStatus status, LocalDateTime threshold);
  Optional<Outbox> findByCorrelationId(String correlationId);
  boolean existsByCorrelationId(String correlationId);
}
