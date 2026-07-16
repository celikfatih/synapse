package dev.synapse.adapter.out.persistence;

import dev.synapse.adapter.out.persistence.entity.TaskDocument;
import dev.synapse.adapter.out.persistence.repository.MongoTaskRepository;
import dev.synapse.domain.task.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoTaskStoreAdapterTest {

    @Mock
    private MongoTaskRepository mongoTaskRepository;

    @InjectMocks
    private MongoTaskStoreAdapter mongoTaskStoreAdapter;

    @Test
    void save_ShouldPersistPullRequestUrlToDocument() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task.attachPullRequestUrl("https://github.com/celikfatih/synapse/pull/42");

        when(mongoTaskRepository.findById(task.getId().getValue())).thenReturn(Optional.empty());

        mongoTaskStoreAdapter.save(task);

        ArgumentCaptor<TaskDocument> captor = ArgumentCaptor.forClass(TaskDocument.class);
        verify(mongoTaskRepository).save(captor.capture());

        TaskDocument savedDoc = captor.getValue();
        assertThat(savedDoc.getPullRequestUrl()).isEqualTo("https://github.com/celikfatih/synapse/pull/42");
    }

    @Test
    void findById_ShouldRestorePullRequestUrlToDomain() {
        TaskDocument doc = TaskDocument.builder()
                .id("test-id-123")
                .requester("dev")
                .message("Fix bug")
                .source("SLACK")
                .correlationId("corr-1")
                .status("ACCEPTED")
                .pullRequestUrl("https://github.com/celikfatih/synapse/pull/42")
                .build();

        when(mongoTaskRepository.findById("test-id-123")).thenReturn(Optional.of(doc));

        Task task = mongoTaskStoreAdapter.findById(new Task.TaskId("test-id-123"));
        assertThat(task.getPullRequestUrl()).isEqualTo(new dev.synapse.domain.task.PullRequestUrl("https://github.com/celikfatih/synapse/pull/42"));
    }
}
