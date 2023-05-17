package org.example.reactormessaging.infrastructure.config;

import com.github.fridujo.rabbitmq.mock.MockChannel;
import com.github.fridujo.rabbitmq.mock.MockConnection;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.github.fridujo.rabbitmq.mock.MockNode;
import com.github.fridujo.rabbitmq.mock.metrics.MetricsCollectorWrapper;
import org.example.reactormessaging.domain.exceptions.RabbitExchangeException;
import org.example.reactormessaging.infrastructure.properties.RabbitConnectionProperties;
import org.example.reactormessaging.infrastructure.properties.ReactorRabbitProperties;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.AMQCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import reactor.rabbitmq.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static reactor.rabbitmq.ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE;

@Configuration
@RequiredArgsConstructor
public class RabbitConnectionConfig {

    private final ReactorRabbitProperties reactorRabbitProperties;

    @Bean("connectionFactory")
    @ConditionalOnProperty(prefix = "spring.reactive", name = "test-mode", havingValue = "false", matchIfMissing = true)
    public ConnectionFactory rabbitConnectionFactory(RabbitConnectionProperties rabbitConnectionProperties) {
        final var connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitConnectionProperties.getHost());
        connectionFactory.setUsername(rabbitConnectionProperties.getUsername());
        connectionFactory.setPassword(rabbitConnectionProperties.getPassword());
        connectionFactory.setAutomaticRecoveryEnabled(true);

        ofNullable(rabbitConnectionProperties.getPort())
                .ifPresent(connectionFactory::setPort);
        ofNullable(rabbitConnectionProperties.getVirtualHost())
                .filter(StringUtils::isNotBlank)
                .ifPresent(connectionFactory::setVirtualHost);

        return connectionFactory;
    }

    @Bean("connectionFactory")
    @ConditionalOnProperty(prefix = "spring.reactive", name = "test-mode", havingValue = "true")
    public ConnectionFactory mockConnectionFactory() {
        return new ExtendedMockConnectionFactory();
    }

    private static class ExtendedMockConnectionFactory extends MockConnectionFactory {

        @Override
        public MockConnection newConnection() {
            MetricsCollectorWrapper metricsCollectorWrapper = MetricsCollectorWrapper.Builder.build(this);
            return new ExtendedMockConnection(mockNode, metricsCollectorWrapper);
        }
    }

    @Bean
    public SendOptions sendOptions() {
        return new SendOptions().exceptionHandler(
                new ExceptionHandlers.RetrySendingExceptionHandler(
                        Duration.ofMillis(reactorRabbitProperties.getConnectionRecovery().getTimeout()),
                        Duration.ofMillis(reactorRabbitProperties.getConnectionRecovery().getWaiting()),
                        CONNECTION_RECOVERY_PREDICATE)
        );
    }

    @Bean
    public Sender sender(@Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        return RabbitFlux.createSender(new SenderOptions().connectionFactory(connectionFactory));
    }

    @Bean
    @DependsOn("sender")
    public Receiver receiver(@Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionFactory(connectionFactory));
    }

    @Bean
    public BiConsumer<Receiver.AcknowledgmentContext, Exception> connectionRecovery() {
        return new ExceptionHandlers.RetryAcknowledgmentExceptionHandler(
                Duration.ofMillis(reactorRabbitProperties.getRetryAck().getTimeout()),
                Duration.ofMillis(reactorRabbitProperties.getRetryAck().getWaiting()),
                CONNECTION_RECOVERY_PREDICATE
        );
    }

    private static class ExtendedMockConnection extends MockConnection {

        private final MockNode mockNode;
        private final MetricsCollectorWrapper metricsCollectorWrapper;

        public ExtendedMockConnection(MockNode mockNode, MetricsCollectorWrapper metricsCollectorWrapper) {
            super(mockNode, metricsCollectorWrapper);
            this.mockNode = mockNode;
            this.metricsCollectorWrapper = metricsCollectorWrapper;
        }

        @Override
        public MockChannel createChannel(int channelNumber) throws AlreadyClosedException {
            if (!isOpen()) {
                throw new AlreadyClosedException(new ShutdownSignalException(false, true, null, this));
            }
            return new ExtendedMockChannel(channelNumber, mockNode, this, metricsCollectorWrapper);
        }
    }

    private static class ExtendedMockChannel extends MockChannel {

        public ExtendedMockChannel(int channelNumber, MockNode node, MockConnection mockConnection,
                                   MetricsCollectorWrapper metricsCollectorWrapper) {
            super(channelNumber, node, mockConnection, metricsCollectorWrapper);
        }

        @Override
        public CompletableFuture<Command> asyncCompletableRpc(Method method) {
            if (method instanceof AMQP.Exchange.Declare) {
                return exchangeDeclare((AMQP.Exchange.Declare) method);
            }
            if (method instanceof AMQP.Queue.Declare) {
                return queueDeclare((AMQP.Queue.Declare) method);
            }
            if (method instanceof AMQP.Queue.Bind) {
                return queueBind((AMQP.Queue.Bind) method);
            }
            if (method instanceof AMQP.Exchange.Bind) {
                return exchangeBind((AMQP.Exchange.Bind) method);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeShutdownListener(ShutdownListener listener) {
        }

        private CompletableFuture<Command> exchangeDeclare(AMQP.Exchange.Declare declare) {
            try {
                return completedFuture(new AMQCommand(exchangeDeclare(
                        declare.getExchange(),
                        declare.getType(),
                        declare.getDurable(),
                        declare.getAutoDelete(),
                        declare.getInternal(),
                        declare.getArguments())));
            } catch (IOException e) {
                throw new RabbitExchangeException(format("Unable to declare exchange [%s]", declare.getExchange()), e);
            }
        }

        private CompletableFuture<Command> queueDeclare(AMQP.Queue.Declare declare) {
            return completedFuture(new AMQCommand(queueDeclare(
                    declare.getQueue(),
                    declare.getDurable(),
                    declare.getExclusive(),
                    declare.getAutoDelete(),
                    declare.getArguments())));
        }

        private CompletableFuture<Command> queueBind(AMQP.Queue.Bind bind) {
            return completedFuture(new AMQCommand(queueBind(
                    bind.getQueue(),
                    bind.getExchange(),
                    bind.getRoutingKey(),
                    bind.getArguments())));
        }

        private CompletableFuture<Command> exchangeBind(AMQP.Exchange.Bind bind) {
            return completedFuture(new AMQCommand(exchangeBind(
                    bind.getDestination(),
                    bind.getSource(),
                    bind.getRoutingKey(),
                    bind.getArguments())));
        }
    }

}
