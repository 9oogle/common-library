package com.goggles.common.event.scheduler;


import com.goggles.common.domain.Outbox;
import com.goggles.common.domain.OutboxRepository;
import com.goggles.common.domain.OutboxStatus;
import com.goggles.common.event.OutboxStatusUpdater;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

	private static final int MAX_RETRY = 3;
	private static final int PROCESSING_STUCK_MINUTES = 5;

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final OutboxStatusUpdater outboxStatusUpdater;

	@Scheduled(fixedDelay = 10000)
	@Transactional
	public void relay() {
		List<Outbox> targets = new ArrayList<>();
		targets.addAll(outboxRepository.findByStatusInAndRetryCountLessThan(
				List.of(OutboxStatus.PENDING, OutboxStatus.FAILED), MAX_RETRY));

		// 서버 재시작 등으로 PROCESSING 상태가 일정 시간 이상 유지된 row 복구
		List<Outbox> stuckTargets = outboxRepository.findByStatusAndUpdatedAtBefore(
				OutboxStatus.PROCESSING, LocalDateTime.now().minusMinutes(PROCESSING_STUCK_MINUTES));
		if (!stuckTargets.isEmpty()) {
			log.warn("[Outbox] PROCESSING stuck 감지 {}건 — 재처리합니다", stuckTargets.size());
			targets.addAll(stuckTargets);
		}

		if (targets.isEmpty()) return;

		log.info("[Outbox] 재전송 대상 {}건", targets.size());

		for (Outbox outbox : targets) {
			// 다른 인스턴스가 같은 row를 집어가지 못하도록 PROCESSING으로 선점
			outbox.processing();
			outboxRepository.saveAndFlush(outbox);

			UUID id = outbox.getId();
			try {
				kafkaTemplate.send(outbox.getEventType(), outbox.getCorrelationId(), outbox.getPayload())
						.whenComplete((result, e) -> outboxStatusUpdater.updateRelayStatus(id, e == null));
			} catch (Exception e) {
				outboxStatusUpdater.updateRelayStatus(id, false);
				log.error("[Outbox] 재전송 실패 - outboxId={}", id, e);
			}
		}
	}
}