package io.github.graphqly.reflector.annotations;

import io.github.graphqly.reflector.metadata.strategy.value.DefaultValueProvider;
import io.github.graphqly.reflector.metadata.strategy.value.JsonDefaultValueProvider;
import io.github.graphqly.reflector.util.ReservedStrings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GraphQLArgument {

  String name();

  String description() default "";

  String defaultValue() default ReservedStrings.NULL;

  Class<? extends DefaultValueProvider> defaultValueProvider() default
      JsonDefaultValueProvider.class;
}
