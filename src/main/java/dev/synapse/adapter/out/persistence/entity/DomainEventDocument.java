package dev.synapse.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "domainEvent")
public class DomainEventDocument {

    @MongoId
    private String id;

    private String aggregateId;
    private String correlationId;
    private String eventType;
    private String payload;
    private String status;
    private Instant createdAt;
    private int retryCount;

    @Version
    private Long version;
}
