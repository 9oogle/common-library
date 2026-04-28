package com.goggles.common.event;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class Events {
	private final ApplicationEventPublisher eventPublisher;
	private static ApplicationEventPublisher publisher;

	@PostConstruct
	public void init() {
		publisher = this.eventPublisher;
	}

	public static void trigger(String correlationId, String domainType, String eventType,
			Object payload) {
		publisher.publishEvent(new OutboxEvent(correlationId, domainType, eventType, payload));
	}
}
