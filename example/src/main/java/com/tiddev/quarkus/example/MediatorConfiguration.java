package com.tiddev.quarkus.example;

import com.tiddev.quarkus.mediator.MediatorDefinition;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Declares the example mediator scope for the example resource.
 */
@ApplicationScoped
@MediatorDefinition(name = "orders", packages = "com.tiddev.quarkus.example")
public class MediatorConfiguration {
}
