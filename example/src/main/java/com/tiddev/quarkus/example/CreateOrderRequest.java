package com.tiddev.quarkus.example;

/**
 * Request payload for creating an order.
 *
 * @param customer the customer name
 * @param item the item being ordered
 * @param quantity the quantity requested
 */
public record CreateOrderRequest(String customer, String item, int quantity) {
}
