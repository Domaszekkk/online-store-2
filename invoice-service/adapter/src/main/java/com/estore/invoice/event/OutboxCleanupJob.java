package com.estore.invoice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupJob {
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Value("${outbox.cleanup.retention-period}")
    private Duration retentionPeriod;

    @Scheduled(cron = "${outbox.cleanup.cron}")
    @Transactional
    public void deletePublishedEvents() {
        int deletedEvents = outboxEventJpaRepository.deletePublishedBefore(Instant.now().minus(retentionPeriod));
        if (deletedEvents > 0) {
            log.info("Deleted {} published outbox events older than {}", deletedEvents, retentionPeriod);
        }
    }
}
