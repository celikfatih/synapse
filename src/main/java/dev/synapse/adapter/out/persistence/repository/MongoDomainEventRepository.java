package dev.synapse.adapter.out.persistence.repository;

import dev.synapse.adapter.out.persistence.entity.DomainEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoDomainEventRepository extends MongoRepository<DomainEventDocument, String> {
    List<DomainEventDocument> findAllByStatusOrderByCreatedAtAsc(String status);
}
