package com.tiddev.quarkus.mediator;

/**
 * Intercepts a mediator request before it reaches the handler.
 *
 * @param <TRequest> the request type
 * @param <TResponse> the response type
 */
@FunctionalInterface
public interface RequestMiddleware<TRequest, TResponse> {
    /**
     * Handles the request and optionally invokes the next step in the chain.
     *
     * @param context the mediator context
     * @param chain the remaining invocation
     * @return the result of the middleware or the underlying handler
     */
    TResponse handle(MediatorContext<TRequest> context, MediatorRequestChain<TRequest, TResponse> chain);
}
