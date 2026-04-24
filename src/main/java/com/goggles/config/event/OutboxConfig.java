package com.goggles.config.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goggles.common.domain.InboxRepository;
import com.goggles.common.domain.OutboxRepository;
import com.goggles.common.event.OutboxEventListener;
import com.goggles.common.event.OutboxStatusUpdater;
import com.goggles.common.event.advice.InboxAdvice;
import com.goggles.common.event.scheduler.InboxCleanupScheduler;
import com.goggles.common.event.scheduler.OutboxRelayScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class OutboxConfig {

  @Bean
  @ConditionalOnBean(type = "org.springframework.kafka.core.KafkaTemplate")
  public OutboxStatusUpdater outboxStatusUpdater(OutboxRepository outboxRepository,
      KafkaTemplate<String, Object> kafkaTemplate) {
    return new OutboxStatusUpdater(outboxRepository, kafkaTemplate);
  }

  @Bean
  @ConditionalOnBean(type = "org.springframework.kafka.core.KafkaTemplate")
  public OutboxEventListener outboxEventListener(OutboxRepository outboxRepository,
      KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper,
      OutboxStatusUpdater outboxStatusUpdater) {
    return new OutboxEventListener(outboxRepository, kafkaTemplate, objectMapper,
        outboxStatusUpdater);
  }

  @Bean
  @ConditionalOnBean(type = "org.springframework.kafka.core.KafkaTemplate")
  public OutboxRelayScheduler outboxRelayScheduler(OutboxRepository outboxRepository,
      KafkaTemplate<String, Object> kafkaTemplate, OutboxStatusUpdater outboxStatusUpdater) {
    return new OutboxRelayScheduler(outboxRepository, kafkaTemplate, outboxStatusUpdater);
  }

  @Bean
  @ConditionalOnBean(type = "org.springframework.kafka.core.KafkaTemplate")
  public InboxAdvice inboxAdvice(InboxRepository inboxRepository) {
    return new InboxAdvice(inboxRepository);
  }

  @Bean
  @ConditionalOnBean(type = "org.springframework.kafka.core.KafkaTemplate")
  public InboxCleanupScheduler inboxCleanupScheduler(InboxRepository inboxRepository) {
    return new InboxCleanupScheduler(inboxRepository);
  }
}
