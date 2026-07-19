package com.tiddev.quarkus.mediator;

/**
 * Handles mediator notifications.
 *
 * @param <TNotification> the notification type
 */
public interface NotificationHandler<TNotification> {
    /**
     * Handles the notification.
     *
     * @param notification the notification to handle
     */
    void handle(TNotification notification);
}
