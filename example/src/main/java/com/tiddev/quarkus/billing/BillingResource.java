package com.tiddev.quarkus.billing;

import com.tiddev.quarkus.mediator.Mediator;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that demonstrates a second mediator scope.
 */
@Path("/billing")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BillingResource {

    @Inject
    @Named("billing")
    Mediator mediator;

    @POST
    public InvoiceResponse create(InvoiceRequest request) {
        InvoiceResponse response = mediator.send(request);
        mediator.publish(new InvoiceIssuedNotification(response.invoiceId(), request.customer(), request.amount()));
        return response;
    }
}
