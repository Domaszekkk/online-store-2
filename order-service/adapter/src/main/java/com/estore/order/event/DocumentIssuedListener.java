package com.estore.order.event;

import com.estore.order.Order;
import com.estore.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIssuedListener {
    private final OrderService orderService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${document.events.topic}")
    public void onDocumentIssued(ConsumerRecord<String, String> record) {
        Header eventType = record.headers().lastHeader(OutboxPublisher.EVENT_TYPE_HEADER);
        if (eventType != null && !DocumentIssuedEvent.TYPE.equals(new String(eventType.value(), StandardCharsets.UTF_8))) {
            log.debug("Skipping event of type {} with key {}", new String(eventType.value(), StandardCharsets.UTF_8), record.key());
            return;
        }
        DocumentIssuedEvent event = jsonMapper.readValue(record.value(), DocumentIssuedEvent.class);
        Order order = orderService.confirmOrder(event.orderId(), event.documentNumber());
        log.info("Order {} is {} with document {}", order.getOrderNumber(), order.getStatus(), event.documentNumber());
    }
}
