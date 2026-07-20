package com.tiddev.quarkus.order;

import com.tiddev.quarkus.mediator.MediatorNotificationHandler;
import com.tiddev.quarkus.mediator.NotificationHandler;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

/**
 * Logs order-created notifications for the example mediator scope.
 */
@ApplicationScoped
@MediatorNotificationHandler
public class OrderCreatedAuditHandler implements NotificationHandler<OrderCreatedNotification> {

    private static final Logger LOG = Logger.getLogger(OrderCreatedAuditHandler.class.getName());

    @Override
    public void handle(OrderCreatedNotification notification) {
        LOG.info(() -> "Audit order " + notification.orderId() + " for " + notification.customer());
    }
}
