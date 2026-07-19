package com.tiddev.quarkus.mediator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a CDI bean as a mediator definition and declares the package roots it owns.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MediatorDefinition {
    /**
     * Returns the optional CDI name for the generated mediator bean.
     *
     * @return the mediator name
     */
    String name() default "";

    /**
     * Returns the package roots that belong to this mediator definition.
     *
     * @return the package roots
     */
    String[] packages() default {};
}
