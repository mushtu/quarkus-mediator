package com.tiddev.quarkus.billing;

import com.tiddev.quarkus.mediator.MediatorRequestHandler;
import com.tiddev.quarkus.mediator.RequestHandler;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Creates a new invoice response for the billing mediator scope.
*/
@ApplicationScoped
@MediatorRequestHandler
public class CreateInvoiceHandler implements RequestHandler<InvoiceRequest, InvoiceResponse> {

    @Override
    public InvoiceResponse handle(InvoiceRequest request) {
        String invoiceId = UUID.randomUUID().toString();
        return new InvoiceResponse(invoiceId, "Issued invoice for " + request.customer() + " in the amount of " + request.amount());
    }
}
