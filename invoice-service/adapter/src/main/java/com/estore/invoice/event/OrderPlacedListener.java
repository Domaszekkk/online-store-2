package com.estore.invoice.event;

import com.estore.invoice.Document;
import com.estore.invoice.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedListener {
    private static final String EVENT_TYPE_HEADER = "eventType";

    private final DocumentService documentService;
    private final OrderPlacedEventMapper orderPlacedEventMapper;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${order.events.topic}")
    @Transactional
    public void onOrderPlaced(ConsumerRecord<String, String> record) {
        Header eventType = record.headers().lastHeader(EVENT_TYPE_HEADER);
        if (eventType != null && !OrderPlacedEvent.TYPE.equals(new String(eventType.value(), StandardCharsets.UTF_8))) {
            log.debug("Skipping event of type {} with key {}", new String(eventType.value(), StandardCharsets.UTF_8), record.key());
            return;
        }
        OrderPlacedEvent event = jsonMapper.readValue(record.value(), OrderPlacedEvent.class);
        Document document = documentService.issue(orderPlacedEventMapper.toCommand(event));
        log.info("Document {} ({}) is issued for order {}", document.getNumber(), document.getType(), event.orderNumber());
    }
}
