package com.goggles.common.event;

public record OutboxEvent(
		String correlationId,
		String domainType,
		String eventType,
		Object payload
) {}
