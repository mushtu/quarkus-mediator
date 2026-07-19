package com.tiddev.quarkus.mediator;

/**
 * Executes request and notification flows for a mediator scope.
 */
public interface Mediator {
    /**
     * Sends a request to the matching request handler and returns its response.
     *
     * @param request the request to dispatch
     * @param <TResponse> the expected response type
     * @return the handler response
     */
    <TResponse> TResponse send(Object request);

    /**
     * Publishes a notification to all matching notification handlers.
     *
     * @param notification the notification to publish
     */
    void publish(Object notification);
}
