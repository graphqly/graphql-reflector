package io.github.graphqly.reflector.generator.mapping.common;

import graphql.introspection.Introspection;
import io.github.graphqly.reflector.annotations.GraphQLDirective;
import io.github.graphqly.reflector.execution.Directives;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.Utils;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjector;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorParams;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.introspection.Introspection.DirectiveLocation.*;

public class DirectiveValueDeserializer implements ArgumentInjector {

  private static final Introspection.DirectiveLocation[] SORTED_LOCATIONS = {
    FIELD, INLINE_FRAGMENT, FRAGMENT_SPREAD, FRAGMENT_DEFINITION, QUERY, MUTATION
  };

  @Override
  public Object getArgumentValue(ArgumentInjectorParams params) {
    GraphQLDirective descriptor = params.getParameter().getAnnotation(GraphQLDirective.class);
    boolean allDirectives = ClassUtils.isSuperClass(Collection.class, params.getType());
    ResolutionEnvironment env = params.getResolutionEnvironment();
    String fallBackDirectiveName =
        env.globalEnvironment.typeInfoGenerator.generateDirectiveTypeName(
            params.getBaseType(), env.globalEnvironment.messageBundle);
    String directiveName = Utils.coalesce(descriptor.name(), fallBackDirectiveName);
    Stream<Introspection.DirectiveLocation> locations =
        descriptor.locations().length != 0
            ? Arrays.stream(descriptor.locations())
            : sortedLocations(
                params
                    .getResolutionEnvironment()
                    .dataFetchingEnvironment
                    .getGraphQLSchema()
                    .getDirective(directiveName)
                    .validLocations());
    Directives directives = env.getDirectives();
    Stream<Map<String, Object>> rawValues =
        locations
            .map(loc -> directives.find(loc, directiveName))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream);
    Object deserializableValue =
        allDirectives ? rawValues.collect(Collectors.toList()) : rawValues.findFirst().orElse(null);
    if (deserializableValue == null) {
      return null;
    }
    return params
        .getResolutionEnvironment()
        .valueMapper
        .fromInput(deserializableValue, params.getType());
  }

  @Override
  public boolean supports(AnnotatedType type, Parameter parameter) {
    return parameter != null && parameter.isAnnotationPresent(GraphQLDirective.class);
  }

  private Stream<Introspection.DirectiveLocation> sortedLocations(
      Set<Introspection.DirectiveLocation> locations) {
    return Arrays.stream(SORTED_LOCATIONS)
        .map(loc -> locations.contains(loc) ? loc : null)
        .filter(Objects::nonNull);
  }
}
