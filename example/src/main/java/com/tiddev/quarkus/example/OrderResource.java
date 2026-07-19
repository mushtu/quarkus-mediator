package com.tiddev.quarkus.example;

import com.tiddev.quarkus.mediator.Mediator;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that demonstrates mediator request and notification dispatch.
 */
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    @Named("orders")
    Mediator mediator;

    @POST
    public CreateOrderResponse create(CreateOrderRequest request) {
        CreateOrderResponse response = mediator.send(request);
        mediator.publish(new OrderCreatedNotification(response.orderId(), request.customer(), request.item(), request.quantity()));
        return response;
    }
}
