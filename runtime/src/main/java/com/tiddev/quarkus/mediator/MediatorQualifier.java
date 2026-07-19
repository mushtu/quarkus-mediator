package com.tiddev.quarkus.mediator;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies a generated mediator bean for a specific configuration class.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface MediatorQualifier {
    /**
     * Returns the configuration class associated with the mediator.
     *
     * @return the configuration class
     */
    Class<?> value();
}
