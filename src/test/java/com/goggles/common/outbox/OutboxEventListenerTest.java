package com.goggles.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goggles.common.domain.Outbox;
import com.goggles.common.domain.OutboxRepository;
import com.goggles.common.domain.OutboxStatus;
import com.goggles.common.event.OutboxEvent;
import com.goggles.common.event.OutboxEventListener;
import com.goggles.common.event.OutboxStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class OutboxEventListenerTest {

	private static final OutboxEvent EVENT =
			new OutboxEvent("corr-1", "ORDER", "OrderCreatedEvent", "{\"amount\":1000}");
	@Mock
	OutboxRepository outboxRepository;
	@Mock
	KafkaTemplate<String, Object> kafkaTemplate;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	OutboxStatusUpdater outboxStatusUpdater;
	@InjectMocks
	OutboxEventListener listener;

	// ── recordOutbox ──────────────────────────────────────────────────────────

	@Test
	void 새로운_correlationId면_PENDING으로_저장한다() throws Exception {
		given(outboxRepository.existsByCorrelationId("corr-1")).willReturn(false);
		given(objectMapper.writeValueAsString(any())).willReturn("{\"amount\":1000}");

		listener.recordOutbox(EVENT);

		verify(outboxRepository).save(argThat(o -> "corr-1".equals(o.getCorrelationId()) &&
				OutboxStatus.PENDING.equals(o.getStatus())));
	}

	@Test
	void 중복된_correlationId면_저장하지_않는다() {
		given(outboxRepository.existsByCorrelationId("corr-1")).willReturn(true);

		listener.recordOutbox(EVENT);

		verify(outboxRepository, never()).save(any());
	}

	@Test
	void 직렬화_실패시_Outbox를_저장하지_않는다() throws Exception {
		given(outboxRepository.existsByCorrelationId("corr-1")).willReturn(false);
		given(objectMapper.writeValueAsString(any())).willThrow(
				new JsonProcessingException("직렬화 실패") {});

		listener.recordOutbox(EVENT);

		verify(outboxRepository, never()).save(any());
	}

	// ── publish ───────────────────────────────────────────────────────────────

	@Test
	void Kafka_발행_성공시_handleSuccess를_호출한다() {
		Outbox outbox = buildOutbox(UUID.randomUUID(), OutboxStatus.PENDING);
		given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.of(outbox));
		given(kafkaTemplate.send(
				any(org.apache.kafka.clients.producer.ProducerRecord.class))).willReturn(
				CompletableFuture.completedFuture(mock(SendResult.class)));

		listener.publish(EVENT);

		verify(outboxStatusUpdater).handleSuccess("corr-1");
	}

	@Test
	void Kafka_발행_실패시_handleFailure를_호출한다() {
		Outbox outbox = buildOutbox(UUID.randomUUID(), OutboxStatus.PENDING);
		given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.of(outbox));
		given(kafkaTemplate.send(
				any(org.apache.kafka.clients.producer.ProducerRecord.class))).willReturn(
				CompletableFuture.failedFuture(new RuntimeException("Kafka 연결 실패")));

		listener.publish(EVENT);

		verify(outboxStatusUpdater).handleFailure(eq(EVENT), any(Throwable.class));
	}

	@Test
	void Outbox가_없으면_Kafka_발행하지_않는다() {
		given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.empty());

		listener.publish(EVENT);

		verifyNoInteractions(kafkaTemplate);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private Outbox buildOutbox(UUID id, OutboxStatus status) {
		Outbox outbox = Outbox.builder()
				.correlationId("corr-1")
				.domainType("ORDER")
				.eventType("OrderCreatedEvent")
				.payload("{\"amount\":1000}")
				.status(status)
				.build();
		ReflectionTestUtils.setField(outbox, "id", id);
		return outbox;
	}
}