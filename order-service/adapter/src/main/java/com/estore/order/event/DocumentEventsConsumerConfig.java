package com.estore.order.event;

import com.estore.order.OrderNotFoundException;
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
public class DocumentEventsConsumerConfig {

    @Bean
    public NewTopic documentIssuedDeadLetterTopic(@Value("${document.events.dead-letter-topic}") String deadLetterTopic) {
        return TopicBuilder.name(deadLetterTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler documentEventsErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                                         @Value("${document.events.dead-letter-topic}") String deadLetterTopic,
                                                         @Value("${document.events.retry.interval-millis}") long retryIntervalMillis,
                                                         @Value("${document.events.retry.max-attempts}") long maxRetryAttempts) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, -1));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(retryIntervalMillis, maxRetryAttempts));
        errorHandler.addNotRetryableExceptions(JacksonException.class, IllegalArgumentException.class,
                NullPointerException.class, OrderNotFoundException.class);
        return errorHandler;
    }
}
