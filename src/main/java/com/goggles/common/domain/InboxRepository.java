package com.goggles.common.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<Inbox, UUID> {
	boolean existsByMessageIdAndMessageGroup(UUID messageId, String messageGroup);

	long deleteAllByCreatedAtBefore(LocalDateTime localDateTime);
}