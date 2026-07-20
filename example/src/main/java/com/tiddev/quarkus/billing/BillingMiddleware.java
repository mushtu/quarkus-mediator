package com.tiddev.quarkus.billing;

import com.tiddev.quarkus.mediator.MediatorContext;
import com.tiddev.quarkus.mediator.MediatorRequestChain;
import com.tiddev.quarkus.mediator.MediatorRequestMiddleware;
import com.tiddev.quarkus.mediator.RequestMiddleware;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Logs request execution before and after the billing handler runs.
 */
@ApplicationScoped
@MediatorRequestMiddleware(order = 0)
public class BillingMiddleware implements RequestMiddleware<InvoiceRequest, InvoiceResponse> {

    private static final Logger LOG = Logger.getLogger(BillingMiddleware.class.getName());

    @Override
    public InvoiceResponse handle(MediatorContext<InvoiceRequest> context, MediatorRequestChain<InvoiceRequest, InvoiceResponse> chain) {
        LOG.info(() -> "Billing mediator packages " + context.packages() + " handling " + context.messageType().getSimpleName());
        InvoiceResponse result = chain.next(context.message());
        LOG.info(() -> "Billing mediator packages " + context.packages() + " completed " + context.messageType().getSimpleName());
        return result;
    }
}
