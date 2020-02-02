package io.github.graphqly.reflector.annotations;

import io.github.graphqly.reflector.util.ReservedStrings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Created by bojan.tomic on 3/2/16. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GraphQLQuery {

  String name() default "";

  String description() default "";

  String deprecationReason() default ReservedStrings.NULL;
}
