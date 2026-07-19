package com.tiddev.quarkus.billing;

import com.tiddev.quarkus.mediator.MediatorDefinition;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Declares the billing mediator scope for the billing example.
 */
@ApplicationScoped
@MediatorDefinition(name = "billing", packages = "com.tiddev.quarkus.billing")
public class BillingMediatorConfiguration {
}
