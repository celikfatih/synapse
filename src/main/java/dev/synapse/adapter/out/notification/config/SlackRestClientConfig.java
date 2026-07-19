package dev.synapse.adapter.out.notification.config;

import dev.synapse.shared.config.SlackProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SlackRestClientConfig {

    @Bean
    RestClient slackRestClient(SlackProperties slackProperties) {
        RestClient.Builder builder = RestClient.builder();
        if (slackProperties != null && slackProperties.getWebhookUrl() != null && !slackProperties.getWebhookUrl().isBlank()) {
            builder.baseUrl(slackProperties.getWebhookUrl());
        }
        return builder.build();
    }
}
