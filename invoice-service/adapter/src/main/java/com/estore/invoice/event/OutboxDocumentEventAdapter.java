package com.estore.invoice.event;

import com.estore.invoice.Document;
import com.estore.invoice.DocumentEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxDocumentEventAdapter implements DocumentEventPort {
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void documentIssued(Document document) {
        DocumentIssuedEvent event = new DocumentIssuedEvent(
                document.getOrderId(), document.getNumber(), document.getType().name());
        outboxEventJpaRepository.save(OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(document.getOrderId())
                .eventType(DocumentIssuedEvent.TYPE)
                .payload(jsonMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .build());
    }
}
