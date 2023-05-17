package org.example.reactormessaging.properties;

import org.example.reactormessaging.infrastructure.properties.ReactorRabbitProperties;
import org.example.reactormessaging.utils.TestBase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.application.name=my-app"
})
public class ReactorRabbitPropertiesTest extends TestBase {
    @Autowired
    private ReactorRabbitProperties settings;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void settingsAreLoaded() {
        assertNotNull(settings);
    }

    @Test
    void queuesSettingsAreLoadedCorrectly() {

        assertNotNull(settings.getBindings());

        var expectedQueues = Map.of(
                "fromRouted", "executionRouted.my-app",
                "fromCompleted", "executionCompleted.my-app",
                "fromRejected", "executionRejected.my-app.1",
                "fromCustom", "customMessages.my-app",
                "fromError", "errorExchange.my-app"
        );

        assertEquals(expectedQueues.size(), settings.getBindings().size());

        expectedQueues
                .forEach((key, value) -> {
                    var queue = settings.getBindings().get(key);
                    assertNotNull(queue);
                    assertEquals(value, queue.getQueueName());
                    assertTrue(StringUtils.isNotBlank(queue.getRoutingKey()));
                    assertTrue(StringUtils.isNotBlank(queue.getExchangeName()));
                    assertDoesNotThrow(() -> applicationContext.getBean(key));
                });
    }

    @Test
    void connectionRecoverySettingsAreLoadedCorrectly() {
        var connectionRecovery = settings.getConnectionRecovery();
        assertNotNull(connectionRecovery);
        assertEquals(456, connectionRecovery.getTimeout());
        assertEquals(123, connectionRecovery.getWaiting());
    }

    @Test
    void retryAckRecoverySettingsAreLoadedCorrectly() {
        var retryAck = settings.getRetryAck();
        assertNotNull(retryAck);
        assertEquals(300_000, retryAck.getTimeout());
        assertEquals(1000, retryAck.getWaiting());
    }
}
