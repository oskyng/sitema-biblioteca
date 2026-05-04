package org.example.util;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.logging.Level;

public class EventGridUtil {

    private static EventGridPublisherClient<EventGridEvent> client;

    private static EventGridPublisherClient<EventGridEvent> getClient(ExecutionContext context) {
        if (client == null) {
            String endpoint = System.getenv("EVENT_GRID_ENDPOINT");
            String key = System.getenv("EVENT_GRID_KEY");

            if (endpoint == null || key == null) {
                context.getLogger().warning("Event Grid configuration missing (EVENT_GRID_ENDPOINT or EVENT_GRID_KEY). Events will not be published.");
                return null;
            }

            client = new EventGridPublisherClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildEventGridEventPublisherClient();
        }
        return client;
    }

    public static void publishEvent(String subject, String eventType, Object data, ExecutionContext context) {
        try {
            EventGridPublisherClient<EventGridEvent> publisherClient = getClient(context);
            if (publisherClient == null) return;

            EventGridEvent event = new EventGridEvent(
                subject,
                eventType,
                BinaryData.fromObject(data),
                "1.0"
            );

            publisherClient.sendEvent(event);
            context.getLogger().info("Event published: " + eventType);
        } catch (Exception e) {
            context.getLogger().log(Level.SEVERE, "Failed to publish event to Event Grid", e);
        }
    }
}
