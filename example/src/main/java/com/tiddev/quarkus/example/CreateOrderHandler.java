package com.tiddev.quarkus.example;

import com.tiddev.quarkus.mediator.MediatorRequestHandler;
import com.tiddev.quarkus.mediator.RequestHandler;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Creates a new order response for the example mediator scope.
 */
@ApplicationScoped
@MediatorRequestHandler
public class CreateOrderHandler implements RequestHandler<CreateOrderRequest, CreateOrderResponse> {

    @Override
    public CreateOrderResponse handle(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        return new CreateOrderResponse(orderId,
                "Created order for " + request.quantity() + " x " + request.item() + " for " + request.customer());
    }
}
