package org.example.functions.eventos;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.azure.messaging.eventgrid.EventGridEvent;

/**
 * Azure Functions with Event Grid trigger.
 */
public class EventGridPublisherFunction {
    /**
     * This function will be invoked when an event is received from Event Grid.
     */
    @FunctionName("EventGridPublisherFunction")
    public void run(@EventGridTrigger(name = "eventGridEvent") String eventJson, final ExecutionContext context) {
        context.getLogger().info("Java Event Grid trigger function executed (Publisher renamed).");
        try {
            EventGridEvent event = EventGridEvent.fromString(eventJson).get(0);
            context.getLogger().info("Event Subject: " + event.getSubject());
            context.getLogger().info("Event Type: " + event.getEventType());
        } catch (Exception e) {
            context.getLogger().severe("Error al procesar el evento: " + e.getMessage());
        }
    }
}
