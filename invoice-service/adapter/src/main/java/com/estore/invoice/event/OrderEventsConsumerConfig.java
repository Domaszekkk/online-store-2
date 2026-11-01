package com.estore.invoice.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class OrderEventsConsumerConfig {

    @Bean
    public NewTopic orderPlacedDeadLetterTopic(@Value("${order.events.dead-letter-topic}") String deadLetterTopic) {
        return TopicBuilder.name(deadLetterTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler orderEventsErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                                       @Value("${order.events.dead-letter-topic}") String deadLetterTopic,
                                                       @Value("${order.events.retry.interval-millis}") long retryIntervalMillis,
                                                       @Value("${order.events.retry.max-attempts}") long maxRetryAttempts) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, -1));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(retryIntervalMillis, maxRetryAttempts));
        errorHandler.addNotRetryableExceptions(JacksonException.class, IllegalArgumentException.class, NullPointerException.class);
        return errorHandler;
    }
}
