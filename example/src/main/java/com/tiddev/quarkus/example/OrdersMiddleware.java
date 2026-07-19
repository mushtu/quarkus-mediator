package com.tiddev.quarkus.example;

import com.tiddev.quarkus.mediator.MediatorContext;
import com.tiddev.quarkus.mediator.MediatorMiddleware;
import com.tiddev.quarkus.mediator.MediatorNext;
import com.tiddev.quarkus.mediator.Middleware;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Logs request execution before and after the handler runs.
 */
@ApplicationScoped
@Middleware(order = 0)
public class OrdersMiddleware implements MediatorMiddleware<CreateOrderRequest, CreateOrderResponse> {

    private static final Logger LOG = Logger.getLogger(OrdersMiddleware.class.getName());

    @Override
    public CreateOrderResponse handle(MediatorContext<CreateOrderRequest> context, MediatorNext<CreateOrderRequest, CreateOrderResponse> next) {
        LOG.info(() -> "Mediator packages " + context.packages() + " handling " + context.messageType().getSimpleName());
        CreateOrderResponse result = next.proceed(context.message());
        LOG.info(() -> "Mediator packages " + context.packages() + " completed " + context.messageType().getSimpleName());
        return result;
    }
}
