package com.tiddev.quarkus.order;

import com.tiddev.quarkus.mediator.MediatorContext;
import com.tiddev.quarkus.mediator.MediatorRequestChain;
import com.tiddev.quarkus.mediator.MediatorRequestMiddleware;
import com.tiddev.quarkus.mediator.RequestMiddleware;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Logs request execution before and after the handler runs.
 */
@ApplicationScoped
@MediatorRequestMiddleware(order = 0)
public class OrdersMiddleware implements RequestMiddleware<CreateOrderRequest, CreateOrderResponse> {

    private static final Logger LOG = Logger.getLogger(OrdersMiddleware.class.getName());

    @Override
    public CreateOrderResponse handle(MediatorContext<CreateOrderRequest> context, MediatorRequestChain<CreateOrderRequest, CreateOrderResponse> chain) {
        LOG.info(() -> "Mediator packages " + context.packages() + " handling " + context.messageType().getSimpleName());
        CreateOrderResponse result = chain.next(context.message());
        LOG.info(() -> "Mediator packages " + context.packages() + " completed " + context.messageType().getSimpleName());
        return result;
    }
}
