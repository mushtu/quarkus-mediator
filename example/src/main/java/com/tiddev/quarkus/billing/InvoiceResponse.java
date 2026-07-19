package com.tiddev.quarkus.billing;

/**
 * Response payload returned after generating an invoice.
 *
 * @param invoiceId the generated invoice id
 * @param message a human-readable result message
 */
public record InvoiceResponse(String invoiceId, String message) {
}
