package com.goggles.common.outbox;

import com.goggles.common.domain.Outbox;
import com.goggles.common.domain.OutboxRepository;
import com.goggles.common.domain.OutboxStatus;
import com.goggles.common.event.OutboxEvent;
import com.goggles.common.event.OutboxStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class OutboxStatusUpdaterTest {

    @Mock OutboxRepository outboxRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    OutboxStatusUpdater updater;

    private static final OutboxEvent EVENT = new OutboxEvent(
            "corr-1", "ORDER", "OrderCreatedEvent", "{\"amount\":1000}");

    // ── handleSuccess ─────────────────────────────────────────────────────────

    @Test
    void handleSuccess_호출시_PROCESSED로_변경된다() {
        Outbox outbox = buildOutbox();
        given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.of(outbox));

        updater.handleSuccess("corr-1");

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        verify(outboxRepository).save(outbox);
    }

    @Test
    void handleSuccess_outbox가_없으면_아무것도_하지_않는다() {
        given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.empty());

        updater.handleSuccess("corr-1");

        verify(outboxRepository, never()).save(any());
    }

    // ── handleFailure ─────────────────────────────────────────────────────────

    @Test
    void handleFailure_호출시_FAILED로_변경되고_retryCount가_증가한다() {
        Outbox outbox = buildOutbox();
        given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.of(outbox));

        updater.handleFailure(EVENT, new RuntimeException("Kafka 오류"));

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        verify(outboxRepository).saveAndFlush(outbox);
    }

    @Test
    void handleFailure_MAX_RETRY_초과시_DLT로_전송한다() {
        Outbox outbox = buildOutboxWithRetry(3);
        given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.of(outbox));
        given(kafkaTemplate.send(eq("OrderCreatedEvent.DLT"), any()))
                .willReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        updater.handleFailure(EVENT, new RuntimeException("Kafka 오류"));

        verify(kafkaTemplate).send(eq("OrderCreatedEvent.DLT"), any());
    }

    @Test
    void handleFailure_MAX_RETRY_미만이면_DLT로_전송하지_않는다() {
        Outbox outbox = buildOutbox();
        given(outboxRepository.findByCorrelationId("corr-1")).willReturn(Optional.of(outbox));

        updater.handleFailure(EVENT, new RuntimeException("Kafka 오류"));

        verifyNoInteractions(kafkaTemplate);
    }

    // ── updateRelayStatus ─────────────────────────────────────────────────────

    @Test
    void updateRelayStatus_성공시_PROCESSED로_변경된다() {
        UUID id = UUID.randomUUID();
        Outbox outbox = buildOutbox();
        given(outboxRepository.findById(id)).willReturn(Optional.of(outbox));

        updater.updateRelayStatus(id, true);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        verify(outboxRepository).saveAndFlush(outbox);
    }

    @Test
    void updateRelayStatus_실패시_FAILED로_변경되고_retryCount가_증가한다() {
        UUID id = UUID.randomUUID();
        Outbox outbox = buildOutbox();
        given(outboxRepository.findById(id)).willReturn(Optional.of(outbox));

        updater.updateRelayStatus(id, false);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        verify(outboxRepository).saveAndFlush(outbox);
    }

    @Test
    void updateRelayStatus_outbox가_없으면_아무것도_하지_않는다() {
        UUID id = UUID.randomUUID();
        given(outboxRepository.findById(id)).willReturn(Optional.empty());

        updater.updateRelayStatus(id, true);

        verify(outboxRepository, never()).saveAndFlush(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Outbox buildOutbox() {
        return Outbox.builder()
                .correlationId("corr-1")
                .domainType("ORDER")
                .eventType("OrderCreatedEvent")
                .payload("{\"amount\":1000}")
                .build();
    }

    private Outbox buildOutboxWithRetry(int retryCount) {
        Outbox outbox = buildOutbox();
        for (int i = 0; i < retryCount; i++) outbox.fail();
        return outbox;
    }
}