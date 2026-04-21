package com.goggles.common.event;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goggles.common.domain.Outbox;
import com.goggles.common.domain.OutboxRepository;
import com.goggles.common.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventListener {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final OutboxStatusUpdater outboxStatusUpdater;


	@EventListener
	@Transactional(propagation = Propagation.REQUIRED)
	public void recordOutbox(OutboxEvent event) {

		if (outboxRepository.existsByCorrelationId(event.correlationId())) {
			log.warn("이미 존재하는 correlationId 입니다.: {}", event.correlationId());
			return;
		}

		try {
			String jsonPayload = objectMapper.writeValueAsString(event.payload());

			Outbox outbox = Outbox.builder()
					.correlationId(event.correlationId())
					.domainType(event.domainType())
					.eventType(event.eventType())
					.payload(jsonPayload)
					.status(OutboxStatus.PENDING)
					.build();
			outboxRepository.save(outbox);
		} catch (JsonProcessingException e) {
			log.error("Output payload 직렬화 실패: {}", event.correlationId(), e);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void publish(OutboxEvent event) {
		outboxRepository.findByCorrelationId(event.correlationId())
				.ifPresent(outbox -> {
					// Kafka 메시지에 ID 헤더 추가
					ProducerRecord<String, Object> record =
							new ProducerRecord<>(outbox.getEventType(), null,
									outbox.getCorrelationId(), outbox.getPayload());
					record.headers()
							.add("message_id", outbox.getId()
									.toString()
									.getBytes());

					kafkaTemplate.send(record)
							.whenComplete((result, e) -> {
								if (e == null)
									outboxStatusUpdater.handleSuccess(event.correlationId());
								else outboxStatusUpdater.handleFailure(event, e);
							});
				});
	}

}