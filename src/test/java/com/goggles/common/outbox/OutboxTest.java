package com.goggles.common.outbox;

import com.goggles.common.domain.Outbox;
import com.goggles.common.domain.OutboxStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxTest {

	private Outbox buildOutbox() {
		return Outbox.builder()
				.correlationId("corr-1")
				.domainType("ORDER")
				.eventType("OrderCreatedEvent")
				.payload("{\"amount\":1000}")
				.build();
	}

	@Test
	void 기본_상태는_PENDING이고_retryCount는_0이다() {
		Outbox outbox = buildOutbox();

		assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(outbox.getRetryCount()).isZero();
	}

	@Test
	void processing_호출시_상태가_PROCESSING으로_변경된다() {
		Outbox outbox = buildOutbox();
		outbox.processing();

		assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
	}

	@Test
	void complete_호출시_상태가_PROCESSED로_변경된다() {
		Outbox outbox = buildOutbox();
		outbox.complete();

		assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
	}

	@Test
	void fail_호출시_상태가_FAILED로_변경되고_retryCount가_증가한다() {
		Outbox outbox = buildOutbox();
		outbox.fail();

		assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
		assertThat(outbox.getRetryCount()).isEqualTo(1);
	}

	@Test
	void fail_여러번_호출시_retryCount가_누적된다() {
		Outbox outbox = buildOutbox();
		outbox.fail();
		outbox.fail();
		outbox.fail();

		assertThat(outbox.getRetryCount()).isEqualTo(3);
	}
}