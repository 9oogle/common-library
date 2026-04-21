package com.goggles.common.event;

import com.goggles.common.domain.OutboxRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class OutboxStatusUpdater {

	private static final int MAX_RETRY = 3;
	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleSuccess(String correlationId) {
		outboxRepository.findByCorrelationId(correlationId)
				.ifPresent(outbox -> {
					outbox.complete();
					outboxRepository.save(outbox);
					log.info("Outbox 메세지 전송 및 상태 완료 변경 성공: {}", correlationId);
				});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleFailure(OutboxEvent event, Throwable e) {
		outboxRepository.findByCorrelationId(event.correlationId())
				.ifPresent(outbox -> {
					outbox.fail();
					outboxRepository.saveAndFlush(outbox);

					if (outbox.getRetryCount() >= MAX_RETRY) {
						log.error("최대 재시도 횟수 초과(Total: {}). DLT로 격리합니다: {}", outbox.getRetryCount(),
								event.correlationId());
						sendToDlt(event, outbox.getPayload());
					} else {
						log.warn("메세지 전송 실패 (재시도 예정 {}/{}): {}", outbox.getRetryCount(), MAX_RETRY,
								event.correlationId());
					}
				});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateRelayStatus(UUID id, boolean isSuccess) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			if (isSuccess) {
				outbox.complete();
				log.info("재전송 성공: {}", outbox.getCorrelationId());
			} else {
				outbox.fail();
				log.warn("재전송 실패 (현재 횟수: {}): {}", outbox.getRetryCount(), outbox.getCorrelationId());
			}
			outboxRepository.saveAndFlush(outbox);
		});
	}

	private void sendToDlt(OutboxEvent event, String payload) {
		String dltTopic = event.eventType() + ".DLT";
		try {
			kafkaTemplate.send(dltTopic, payload)
					.whenComplete((res, e) -> {
						if (e != null) log.error("DLT 전송 실패: {}", event.correlationId(), e);
						else log.info("DLT 전송 성공: {}", event.correlationId());
					});
		} catch (Exception e) {
			log.error("DLT 전송 중 예외 발생: {}", event.correlationId(), e);
		}
	}
}