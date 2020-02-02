package io.github.graphqly.reflector.annotations.types;

import graphql.introspection.Introspection;
import io.github.graphqly.reflector.annotations.GraphQLIgnore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static graphql.introspection.Introspection.DirectiveLocation.*;

@GraphQLIgnore
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GraphQLDirective {

  Introspection.DirectiveLocation[] ALL_SCHEMA =
      new Introspection.DirectiveLocation[] {
        SCHEMA,
        SCALAR,
        OBJECT,
        FIELD_DEFINITION,
        ARGUMENT_DEFINITION,
        INTERFACE,
        UNION,
        ENUM,
        ENUM_VALUE,
        INPUT_OBJECT,
        INPUT_FIELD_DEFINITION
      };

  Introspection.DirectiveLocation[] ALL_CLIENT =
      new Introspection.DirectiveLocation[] {
        QUERY, MUTATION, FIELD, FRAGMENT_DEFINITION, FRAGMENT_SPREAD, INLINE_FRAGMENT
      };

  String name() default "";

  String description() default "";

  Introspection.DirectiveLocation[] locations() default {};
}
