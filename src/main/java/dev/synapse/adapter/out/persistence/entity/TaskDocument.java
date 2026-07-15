package dev.synapse.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "task")
public class TaskDocument {

    @MongoId
    private String id;

    private String message;
    private String correlationId;
    private String source;
    private String status;
    private String requester;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private Instant createdAt;

    private Instant startedAt;
    private Instant completedAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;
}
