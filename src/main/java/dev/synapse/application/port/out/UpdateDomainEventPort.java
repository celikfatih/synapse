package dev.synapse.application.port.out;

public interface UpdateDomainEventPort {
    /**
     * Marks a domain event as published.
     * @param id the id of the event to be marked as published.
     */
    void markAsPublished(String id);

    /**
     * Increments the retry count of a domain event.
     * @param id the id of the event to increment the retry count.
     */
    void incrementRetryCount(String id);
}
