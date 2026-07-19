package com.tiddev.quarkus.mediator;

import java.util.Set;

/**
 * Carries request metadata visible to request middleware.
 *
 * @param <TRequest> the request type
 */
public final class MediatorContext<TRequest> {
    private final Set<String> packages;
    private final TRequest message;
    private final Class<TRequest> messageType;

    /**
     * Creates a new mediator context.
     *
     * @param packages the package roots associated with the mediator
     * @param message the current request
     * @param messageType the runtime type of the message
     */
    public MediatorContext(Set<String> packages, TRequest message, Class<TRequest> messageType) {
        this.packages = Set.copyOf(packages);
        this.message = message;
        this.messageType = messageType;
    }

    /**
     * Returns the mediator package roots.
     *
     * @return the package roots
     */
    public Set<String> packages() {
        return packages;
    }

    /**
     * Returns the current message.
     *
     * @return the current message
     */
    public TRequest message() {
        return message;
    }

    /**
     * Returns the runtime type of the current message.
     *
     * @return the message type
     */
    public Class<TRequest> messageType() {
        return messageType;
    }
}
