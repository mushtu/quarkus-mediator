package com.tiddev.quarkus.order;

/**
 * Response payload returned after creating an order.
 *
 * @param orderId the generated order id
 * @param message a human-readable result message
 */
public record CreateOrderResponse(String orderId, String message) {
}
