package io.github.graphqly.reflector.annotations;

import io.github.graphqly.reflector.execution.TypeResolver;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GraphQLTypeResolver {

  Class<? extends TypeResolver> value();
}
