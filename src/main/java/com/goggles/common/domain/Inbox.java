package com.goggles.common.domain;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(name = "p_inbox", uniqueConstraints = @UniqueConstraint(name = "uk_inbox_message_id_group", columnNames = {
		"messageId", "messageGroup"}), indexes = {
		@Index(name = "idx_inbox_message_group", columnList = "messageGroup"),
		@Index(name = "idx_inbox_processed_at", columnList = "processedAt")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public class Inbox extends BaseTime {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(columnDefinition = "uuid", nullable = false)
	private UUID messageId; // = 생산자의 eventId

	@Column(length = 100)
	private String messageGroup; // topic 또는 consumer group

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime processedAt;
}
