package com.tiddev.quarkus.mediator;

/**
 * Represents the remaining middleware invocation for a mediator request.
 *
 * @param <TRequest> the request type
 * @param <TResponse> the response type
 */
@FunctionalInterface
public interface MediatorRequestChain<TRequest, TResponse> {
    /**
     * Proceeds to the next middleware or the final handler.
     *
     * @param request the current request value
     * @return the result of the next step
     */
    TResponse next(TRequest request);
}
