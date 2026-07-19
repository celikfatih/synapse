package dev.synapse.adapter.in.rest.filter.config;

import dev.synapse.adapter.in.rest.filter.SlackSignatureVerificationFilter;
import dev.synapse.shared.config.SlackProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SlackFilterConfig {

    @Bean
    public FilterRegistrationBean<SlackSignatureVerificationFilter> slackSignatureVerificationFilterRegistration(
            SlackProperties slackProperties) {
        FilterRegistrationBean<SlackSignatureVerificationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SlackSignatureVerificationFilter(slackProperties));
        registrationBean.addUrlPatterns("/api/slack/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}
