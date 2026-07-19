package com.tiddev.quarkus.mediator;

/**
 * Handles mediator requests.
 *
 * @param <TRequest> the request type
 * @param <TResponse> the response type
 */
public interface RequestHandler<TRequest, TResponse> {
    /**
     * Handles the request.
     *
     * @param request the request to handle
     * @return the handler response
     */
    TResponse handle(TRequest request);
}
