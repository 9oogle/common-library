package com.goggles.common.event.scheduler;

import com.goggles.common.domain.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class InboxCleanupScheduler {
    private final InboxRepository inboxRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 정리 작업 진행
    public void cleanupOldMessage() {
        // 7일 이전 목록 삭제
        long count = inboxRepository.deleteAllByCreatedAtBefore(LocalDateTime.now().minusDays(7));

        log.info("7일 경과 Inbox 내역 삭제: {}건 삭제됨", count);
    }
}
