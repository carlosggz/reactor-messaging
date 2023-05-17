package org.example.reactormessaging;

import org.example.reactormessaging.domain.components.ReactorEventPublisher;
import org.example.reactormessaging.domain.models.KafkaMessage;
import org.example.reactormessaging.domain.models.RabbitMessage;
import org.example.reactormessaging.infrastructure.properties.ReactorProperties;
import org.example.reactormessaging.utils.CustomDto;
import org.example.reactormessaging.utils.TestBase;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReactorMessagingApplicationTests extends TestBase {

	@Autowired
	private ReactorEventPublisher eventPublisher;

	@Autowired
	@Qualifier("testSuccessConcurrentCollection")
	private List<String> testSuccessConcurrentCollection;

	@Autowired
	private ReactorProperties reactorProperties;

	@BeforeEach
	void setup() {
		testSuccessConcurrentCollection.clear();
	}

	@Test
	void contextLoads() {
		assertNotNull(reactorProperties);
		assertTrue(reactorProperties.getRetryAttempts() > 0);
		assertTrue(reactorProperties.getRetryDuration() > 0);
	}

	@ParameterizedTest
	@ValueSource(strings = {"executionRouted", "executionCompleted", "executionRejected"})
	void whenRabbitMessageIsSentTheConsumerReceiveIt(String exchange) {
		var message = "Hello at " + LocalDateTime.now();

		eventPublisher
				.sendMessage(RabbitMessage.builder()
						.payload(message)
						.exchangeName(exchange)
						.routingKey("x.my-app." + exchange)
						.build())
				.subscribe();

		Awaitility
				.await()
				.atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> {
					assertEquals(1, testSuccessConcurrentCollection.size());
					assertEquals(message, testSuccessConcurrentCollection.get(0));
				});
	}

	@ParameterizedTest
	@ValueSource(strings = {"events"})
	void whenKafkaMessageIsSentTheConsumerReceiveIt(String topic) {
		var message = "Hello at " + LocalDateTime.now();

		eventPublisher
				.sendMessage(KafkaMessage.builder()
						.topic(topic)
						.routingKey("some-routing-key")
						.payload(message)
						.build())
				.subscribe();

		Awaitility
				.await()
				.atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> {
					assertEquals(1, testSuccessConcurrentCollection.size());
					assertEquals(message, testSuccessConcurrentCollection.get(0));
				});
	}

	@Test
	@SneakyThrows
	void whenCustomTypeRabbitMessageIsSentTheConsumerReceiveIt() {
		var message = CustomDto.builder()
				.id(123)
				.name("Testing")
				.build();

		eventPublisher
				.sendMessage(RabbitMessage.builder()
						.payload(message)
						.exchangeName("customMessages")
						.routingKey("x.my-app.customMessages")
						.build())
				.subscribe();

		Awaitility
				.await()
				.atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> {
					assertEquals(1, testSuccessConcurrentCollection.size());
					assertEquals(message.toString(), testSuccessConcurrentCollection.get(0));
				});
	}

	@Test
	@SneakyThrows
	void whenCustomTypeKafkaMessageIsSentTheConsumerReceiveIt() {
		var message = CustomDto.builder()
				.id(123)
				.name("Testing")
				.build();

		eventPublisher
				.sendMessage(KafkaMessage.builder()
						.topic("dto")
						.routingKey("some-routing-key")
						.payload(message)
						.build())
				.subscribe();

		Awaitility
				.await()
				.atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> {
					assertEquals(1, testSuccessConcurrentCollection.size());
					assertEquals(message.toString(), testSuccessConcurrentCollection.get(0));
				});
	}

	@Test
	void whenRabbitErrorIsRaisedRetriesAreExecuted() {
		var message = "Hello at " + LocalDateTime.now();

		eventPublisher
				.sendMessage(RabbitMessage.builder()
						.payload(message)
						.exchangeName("errorExchange")
						.routingKey("x.my-app.errorExchange")
						.build())
				.subscribe();

		Awaitility
				.await()
				.atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> {
					assertEquals(1+reactorProperties.getRetryAttempts(), testSuccessConcurrentCollection.size());
					testSuccessConcurrentCollection.forEach(m -> assertEquals(message, m));
				});
	}

	@Test
	void whenKafkaErrorIsRaisedRetriesAreExecuted() {
		var message = "Hello at " + LocalDateTime.now();

		eventPublisher
				.sendMessage(KafkaMessage.builder()
						.payload(message)
						.topic("errorTopic")
						.build())
				.subscribe();

		Awaitility
				.await()
				.atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> {
					assertEquals(1+reactorProperties.getRetryAttempts(), testSuccessConcurrentCollection.size());
					testSuccessConcurrentCollection.forEach(m -> assertEquals(message, m));
				});
	}

}
