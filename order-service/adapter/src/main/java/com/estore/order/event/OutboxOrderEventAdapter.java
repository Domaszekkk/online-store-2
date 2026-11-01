package com.estore.order.event;

import com.estore.order.Order;
import com.estore.order.OrderEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxOrderEventAdapter implements OrderEventPort {
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final OrderPlacedEventFactory orderPlacedEventFactory;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void orderPlaced(Order order) {
        OrderPlacedEvent event = orderPlacedEventFactory.create(order);
        outboxEventJpaRepository.save(OutboxEventEntity.builder()
                .id(event.eventId())
                .aggregateId(order.getId())
                .eventType(OrderPlacedEvent.TYPE)
                .payload(jsonMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .build());
    }
}
