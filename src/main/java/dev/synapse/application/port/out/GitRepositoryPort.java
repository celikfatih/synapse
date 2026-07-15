package dev.synapse.application.port.out;

import java.nio.file.Path;

public interface GitRepositoryPort {
    void cloneAndCheckout(String repositoryUrl, Path workspacePath, String branchName);
}
