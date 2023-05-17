package org.example.reactormessaging.infrastructure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitConnectionProperties {

    private String host = "localhost";
    private Integer port = 5672;
    private String virtualHost;

    private String username;
    private String password;
}
