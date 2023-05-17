package org.example.reactormessaging.infrastructure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.reactive")
public class ReactorProperties {
    private static int DEFAULT_RETRY_ATTEMPTS = 2;
    private static int DEFAULT_RETRY_DURATION = 100;
    /**
     * Test mode enable
     * <p>Only available for rabbit mq</p>
     */
    private boolean testMode = false;

    /**
     * Max of retries in case of error processing message
     */
    private int retryAttempts = DEFAULT_RETRY_ATTEMPTS;

    /**
     * Duration in milliseconds between retries
     */
    private int retryDuration = DEFAULT_RETRY_DURATION;

    public int getRetryAttempts() {
        return retryAttempts > 0 ? retryAttempts : DEFAULT_RETRY_ATTEMPTS;
    }

    public int getRetryDuration() {
        return retryDuration > 0 ? retryDuration : DEFAULT_RETRY_DURATION;
    }
}
