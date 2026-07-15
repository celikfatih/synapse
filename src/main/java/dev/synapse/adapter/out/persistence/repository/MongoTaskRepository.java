package dev.synapse.adapter.out.persistence.repository;

import dev.synapse.adapter.out.persistence.entity.TaskDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoTaskRepository extends MongoRepository<TaskDocument, String> {
}
