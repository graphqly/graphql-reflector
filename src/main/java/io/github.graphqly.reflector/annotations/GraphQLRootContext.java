package io.github.graphqly.reflector.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks the parameters representing the root context */
@GraphQLIgnore
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GraphQLRootContext {

  String value() default "";
}
