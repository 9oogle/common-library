package com.goggles.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class Events {
	private final ApplicationEventPublisher eventPublisher;

	public void trigger(String correlationId, String domainType, String eventType, Object payload) {
		eventPublisher.publishEvent(new OutboxEvent(correlationId, domainType, eventType, payload));
	}
}
