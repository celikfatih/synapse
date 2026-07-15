package dev.synapse.adapter.out.jira.config;

import dev.synapse.shared.config.JiraProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JiraProperties.class)
public class JiraRestClientConfig {

    @Bean
    RestClient jiraRestClient(JiraProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.getUsername(), properties.getToken());
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }
}
