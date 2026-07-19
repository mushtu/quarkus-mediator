package com.tiddev.quarkus.example;

/**
 * Notification emitted after an order is created.
 *
 * @param orderId the generated order id
 * @param customer the customer name
 * @param item the item being ordered
 * @param quantity the quantity ordered
 */
public record OrderCreatedNotification(String orderId, String customer, String item, int quantity) {
}
