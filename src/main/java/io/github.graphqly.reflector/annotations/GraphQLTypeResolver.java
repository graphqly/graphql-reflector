package io.github.graphqly.reflector.annotations;

import io.github.graphqly.reflector.execution.TypeResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GraphQLTypeResolver {

  Class<? extends TypeResolver> value();
}
