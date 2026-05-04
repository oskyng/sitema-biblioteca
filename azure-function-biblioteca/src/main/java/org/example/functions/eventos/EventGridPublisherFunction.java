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
    public void run(@EventGridTrigger(name = "eventGridEvent") EventGridEvent event, final ExecutionContext context) {
        context.getLogger().info("Java Event Grid trigger function executed (Publisher renamed).");
        context.getLogger().info("Event Subject: " + event.getSubject());
        context.getLogger().info("Event Type: " + event.getEventType());
    }
}
