package dev.synapse.adapter.in.rest.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Synapse",
                description = "Synapse Api Documentation",
                version = "1.0",
                contact = @Contact(name = "Synapse Dev Team", email = "<member>@synapse.dev", url = "https://synapse.dev")
        ),
        servers = @Server(url = "/")
)
public class OpenApiConfig {
}
