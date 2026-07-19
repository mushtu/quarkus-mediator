package com.tiddev.quarkus.billing;

/**
 * Request payload for generating an invoice.
 *
 * @param customer the customer name
 * @param amount the amount to invoice
 */
public record InvoiceRequest(String customer, double amount) {
}
