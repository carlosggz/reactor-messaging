package org.example.reactormessaging.infrastructure.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.reactive.rabbit")
@NoArgsConstructor
public class ReactorRabbitProperties {

    /**
     * Exchanges list
     */
    private List<ExchangeSettings> exchanges;

    /**
     * Bindings list
     */
    private Map<String, BindingSettings> bindings;

    /**
     * Connection recovery settings
     * Optional with defaults
     */
    private ConnectionRecovery connectionRecovery;

    /**
     * Retry ACK settings
     * Optional with defaults
     */
    private RetryAck retryAck;

    public ConnectionRecovery getConnectionRecovery() {
        if (Objects.isNull(connectionRecovery)) {
            connectionRecovery = new ConnectionRecovery();
        }

        return connectionRecovery;
    }

    public RetryAck getRetryAck() {
        if (Objects.isNull(retryAck)) {
            retryAck = new RetryAck();
        }

        return retryAck;
    }

    @Data
    @NoArgsConstructor
    public static class ExchangeSettings {
        /**
         * Exchange name
         */
        private String name;

        /**
         * Exchange type
         * <p>Optional with default value topic</p>
         */
        private ExchangeType type = ExchangeType.topic;
    }

    @Data
    @NoArgsConstructor
    public static class BindingSettings {

        /**
         * Exchange name
         */
        private String exchangeName;

        /**
         * Group name
         * <p>Your queue will be exchange-name.group-name</p>
         */
        private String groupName;


        /**
         * Delete queue automatically on close app
         * <p>Default to false</p>
         */
        private boolean autoDelete = false;

        /**
         * Use a dead letter queue or not
         * <p>Default to true</p>
         */
        private boolean useDlq = true;

        /**
         * Queue is durable
         * <p>Default to true</p>
         */
        private boolean durable = true;

        /**
         * Routing key
         */
        private String routingKey;

        /**
         * Auto ACK of messages. If false, the manual ack will be used
         * <p>Default to true</p>
         */
        private boolean autoAck = true;

        /**
         * Expected message type
         * <p>Default string</p>
         */
        private String targetType = null;

        public String getQueueName() {
            return String.format("%s.%s", exchangeName, groupName);
        }
    }

    @Data
    @NoArgsConstructor
    public static class ConnectionRecovery {
        /**
         * Default timeout in milliseconds
         * <p>Optional with default value 4 seconds</p>
         */
        private int timeout = 41000; //4 seconds

        /**
         * Default waiting in milliseconds
         * <p>Optional with default value 100 milliseconds</p>
         */
        private int waiting = 100; //100 ms
    }

    @Data
    @NoArgsConstructor
    public static class RetryAck {
        /**
         * Default timeout in milliseconds
         * <p>Optional with default value 5 minutes</p>
         */
        private int timeout = 300_000; //5 minutes

        /**
         * Default waiting in milliseconds
         * <p>Optional with default value 1 second</p>
         */
        private int waiting = 1000; //1 second
    }

    public enum ExchangeType {
        topic,
        direct,
        fanout
    }
}
