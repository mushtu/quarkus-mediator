package com.tiddev.quarkus.billing;

import com.tiddev.quarkus.mediator.MediatorContext;
import com.tiddev.quarkus.mediator.MediatorMiddleware;
import com.tiddev.quarkus.mediator.MediatorNext;
import com.tiddev.quarkus.mediator.Middleware;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Logs request execution before and after the billing handler runs.
 */
@ApplicationScoped
@Middleware(order = 0)
public class BillingMiddleware implements MediatorMiddleware<InvoiceRequest, InvoiceResponse> {

    private static final Logger LOG = Logger.getLogger(BillingMiddleware.class.getName());

    @Override
    public InvoiceResponse handle(MediatorContext<InvoiceRequest> context, MediatorNext<InvoiceRequest, InvoiceResponse> next) {
        LOG.info(() -> "Billing mediator packages " + context.packages() + " handling " + context.messageType().getSimpleName());
        InvoiceResponse result = next.proceed(context.message());
        LOG.info(() -> "Billing mediator packages " + context.packages() + " completed " + context.messageType().getSimpleName());
        return result;
    }
}
