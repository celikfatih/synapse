package dev.synapse.adapter.out.persistence.repository;

import dev.synapse.adapter.out.persistence.entity.DomainEventDocument;
import dev.synapse.domain.event.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MongoDomainEventCustomRepository {

    private final MongoTemplate mongoTemplate;

    public void markAsPublished(String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = Update.update("status", EventStatus.PUBLISHED);
        mongoTemplate.updateFirst(query, update, DomainEventDocument.class);
    }

    public void incrementRetryCount(String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update().inc("retryCount", 1);
        mongoTemplate.updateFirst(query, update, DomainEventDocument.class);
    }
}
