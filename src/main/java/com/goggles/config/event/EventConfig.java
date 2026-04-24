package com.goggles.config.event;

import com.goggles.common.event.Events;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Configuration
public class EventConfig implements AsyncConfigurer {

	@Bean
	public Events events(ApplicationEventPublisher eventPublisher) {
		return new Events(eventPublisher);
	}
}
