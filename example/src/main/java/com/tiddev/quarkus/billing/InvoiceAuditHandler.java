package com.tiddev.quarkus.billing;

import com.tiddev.quarkus.mediator.MediatorNotificationHandler;
import com.tiddev.quarkus.mediator.NotificationHandler;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Logs invoice-issued notifications for the billing mediator scope.
 */
@ApplicationScoped
@MediatorNotificationHandler
public class InvoiceAuditHandler implements NotificationHandler<InvoiceIssuedNotification> {

    private static final Logger LOG = Logger.getLogger(InvoiceAuditHandler.class.getName());

    @Override
    public void handle(InvoiceIssuedNotification notification) {
        LOG.info(() -> "Audit invoice " + notification.invoiceId() + " for " + notification.customer());
    }
}
