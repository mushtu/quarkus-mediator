package com.tiddev.quarkus.order;

import com.tiddev.quarkus.mediator.MediatorDefinition;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Declares the example mediator scope for the example resource.
 */
@ApplicationScoped
@MediatorDefinition(name = "orders", packages = "com.tiddev.quarkus.order")
public class MediatorConfiguration {
}
