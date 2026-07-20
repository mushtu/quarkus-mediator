package com.tiddev.quarkus.mediator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bean as request middleware in a mediator pipeline.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MediatorRequestMiddleware {
    /**
     * Returns the middleware order. Lower values run first.
     *
     * @return the middleware order
     */
    int order() default 0;
}
