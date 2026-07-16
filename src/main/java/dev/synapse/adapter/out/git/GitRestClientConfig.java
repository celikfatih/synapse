package dev.synapse.adapter.out.git;

import dev.synapse.shared.config.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class GitRestClientConfig {

    @Bean
    RestClient gitRestClient(GitProperties gitProperties) {
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeaders(headers -> {
                    if (gitProperties != null && gitProperties.getToken() != null && !gitProperties.getToken().isBlank()) {
                        headers.setBearerAuth(gitProperties.getToken());
                    }
                    headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
                })
                .build();
    }
}
