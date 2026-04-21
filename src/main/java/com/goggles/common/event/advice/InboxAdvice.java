package com.goggles.common.event.advice;

import com.goggles.common.domain.Inbox;
import com.goggles.common.domain.InboxRepository;
import com.goggles.common.event.annotation.IdempotentConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class InboxAdvice {
	private final InboxRepository inboxRepository;

	@Around("@annotation(idempotentConsumer)")
	@Transactional(rollbackFor = Exception.class)
	public Object handle(ProceedingJoinPoint joinPoint, IdempotentConsumer idempotentConsumer)
			throws Throwable {
		UUID messageId = extractMessageId(joinPoint.getArgs());

		if (messageId == null) {
			log.warn("Message ID가 없는 메시지입니다. 멱등성 체크를 진행하지 않습니다.");
			return joinPoint.proceed();
		}

		try {
			// 메서드 실행 성공 시 Inbox에 기록 (messageId는 Outbox에서 발행된 ID이며 동일하게 저장합니다.)
			Inbox inbox = Inbox.builder()
					.messageId(messageId)
					.messageGroup(idempotentConsumer.value())
					.build();

			inboxRepository.saveAndFlush(inbox);
			log.debug("Inbox 기록 성공: {}", messageId);

		} catch (DataIntegrityViolationException e) {
			log.info("이미 처리 중이거나 완료된 중복 메시지입니다: {}", messageId);
			return null;
		}

		try {
			return joinPoint.proceed();
		} catch (Throwable throwable) {
			log.error("메세지 수신 실패, Inbox 기록을 롤백합니다: {}", messageId);
			throw throwable;
		}
	}

	private UUID extractMessageId(Object[] args) {
		for (Object arg : args) {
			// Kafka 리스너에서 ConsumerRecord<K, V>를 파라미터로 받을 경우
			if (arg instanceof ConsumerRecord<?, ?> record) {
				Header header = record.headers()
						.lastHeader("message_id");
				if (header != null) return parseUuid(header.value());
			}

			// Spring Messaging Message 객체인 경우
			if (arg instanceof Message<?> message) {
				Object header = message.getHeaders()
						.get("message_id");
				if (header instanceof byte[] bytes) return parseUuid(bytes);
				if (header instanceof String str) return UUID.fromString(str);
			}
		}
		return null;
	}

	private UUID parseUuid(byte[] value) {
		try {
			return UUID.fromString(new String(value, StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.error("메시지 ID 형식이 유효하지 않습니다:{}", value, e);
			return null;
		}
	}
}
