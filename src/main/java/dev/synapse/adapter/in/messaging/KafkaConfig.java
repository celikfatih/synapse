package dev.synapse.adapter.in.messaging;

import dev.synapse.adapter.in.messaging.interceptor.CorrelationIdRecordInterceptor;
import dev.synapse.domain.event.DomainEvent;
import dev.synapse.shared.config.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    @Bean
    NewTopic testTopic(KafkaProperties kafkaProperties) {
        return TopicBuilder
                .name(kafkaProperties.getTaskTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, DomainEvent> manuelAcknowledgeContainerFactory(
            ConsumerFactory<String, DomainEvent> consumerFactory,
            KafkaProperties kafkaProperties,
            CorrelationIdRecordInterceptor correlationIdRecordInterceptor,
            TaskEventConsumerRecordRecoverer taskEventConsumerRecordRecoverer) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, DomainEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.setRecordInterceptor(correlationIdRecordInterceptor);

        long maxRetries = Math.max(0, kafkaProperties.getMaxAttempts() - 1L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                taskEventConsumerRecordRecoverer,
                new FixedBackOff(kafkaProperties.getBackoffMs(), maxRetries)
        );
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
