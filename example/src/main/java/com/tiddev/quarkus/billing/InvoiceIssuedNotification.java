package com.tiddev.quarkus.billing;

/**
 * Notification emitted after an invoice is generated.
 *
 * @param invoiceId the generated invoice id
 * @param customer the customer name
 * @param amount the invoiced amount
 */
public record InvoiceIssuedNotification(String invoiceId, String customer, double amount) {
}
