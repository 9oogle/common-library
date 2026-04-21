package com.goggles.config.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goggles.common.domain.InboxRepository;
import com.goggles.common.domain.OutboxRepository;
import com.goggles.common.event.Events;
import com.goggles.common.event.OutboxEventListener;
import com.goggles.common.event.OutboxStatusUpdater;
import com.goggles.common.event.advice.InboxAdvice;
import com.goggles.common.event.scheduler.InboxCleanupScheduler;
import com.goggles.common.event.scheduler.OutboxRelayScheduler;
import com.goggles.common.filter.MdcLoggingFilter;
import com.goggles.common.pagination.CommonCursorRequestArgumentResolver;
import com.goggles.common.pagination.CommonPageRequestArgumentResolver;
import com.goggles.common.util.MdcTaskDecorator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.Executor;

@EnableAsync
@Configuration
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EventConfig implements AsyncConfigurer {

	// ── Event ────────────────────────────────────────────────────────────────

	@Bean
	public Events events(ApplicationEventPublisher eventPublisher) {
		return new Events(eventPublisher);
	}

	@Bean
	public OutboxStatusUpdater outboxStatusUpdater(OutboxRepository outboxRepository,
			KafkaTemplate<String, Object> kafkaTemplate) {
		return new OutboxStatusUpdater(outboxRepository, kafkaTemplate);
	}

	@Bean
	public OutboxEventListener outboxEventListener(OutboxRepository outboxRepository,
			KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper,
			OutboxStatusUpdater outboxStatusUpdater) {
		return new OutboxEventListener(outboxRepository, kafkaTemplate, objectMapper,
				outboxStatusUpdater);
	}

	@Bean
	public OutboxRelayScheduler outboxRelayScheduler(OutboxRepository outboxRepository,
			KafkaTemplate<String, Object> kafkaTemplate, OutboxStatusUpdater outboxStatusUpdater) {
		return new OutboxRelayScheduler(outboxRepository, kafkaTemplate, outboxStatusUpdater);
	}

	@Bean
	public InboxAdvice inboxAdvice(InboxRepository inboxRepository) {
		return new InboxAdvice(inboxRepository);
	}

	@Bean
	public InboxCleanupScheduler inboxCleanupScheduler(InboxRepository inboxRepository) {
		return new InboxCleanupScheduler(inboxRepository);
	}

	// ── Filter ───────────────────────────────────────────────────────────────

	@Bean
	public MdcLoggingFilter mdcLoggingFilter() {
		return new MdcLoggingFilter();
	}

	// ── ArgumentResolver ─────────────────────────────────────────────────────

	@Bean
	public WebMvcConfigurer commonArgumentResolverConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
				resolvers.add(new CommonPageRequestArgumentResolver());
				resolvers.add(new CommonCursorRequestArgumentResolver());
			}
		};
	}

	// ── Async ────────────────────────────────────────────────────────────────

	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(50);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("Async-");
		executor.setTaskDecorator(new MdcTaskDecorator());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return new DelegatingSecurityContextAsyncTaskExecutor(executor);
	}
}
