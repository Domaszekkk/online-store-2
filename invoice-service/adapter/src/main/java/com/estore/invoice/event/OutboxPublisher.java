package com.estore.invoice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    public static final String EVENT_TYPE_HEADER = "eventType";
    public static final String EVENT_ID_HEADER = "eventId";

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${document.events.topic}")
    private String topic;

    @Value("${outbox.publisher.batch-size}")
    private int batchSize;

    @Value("${outbox.publisher.send-timeout-millis}")
    private long sendTimeoutMillis;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-millis}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> events = outboxEventJpaRepository.lockUnpublishedBatch(batchSize);
        for (OutboxEventEntity event : events) {
            try {
                send(event);
                event.setPublishedAt(Instant.now());
            } catch (ExecutionException | TimeoutException exception) {
                log.warn("Publishing outbox event {} failed, it will be retried", event.getId(), exception);
                return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void send(OutboxEventEntity event) throws ExecutionException, InterruptedException, TimeoutException {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getAggregateId().toString(), event.getPayload());
        record.headers().add(EVENT_TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EVENT_ID_HEADER, event.getId().toString().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get(sendTimeoutMillis, TimeUnit.MILLISECONDS);
    }
}
