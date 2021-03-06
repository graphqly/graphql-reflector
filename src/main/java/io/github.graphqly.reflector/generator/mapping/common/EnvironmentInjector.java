package io.github.graphqly.reflector.generator.mapping.common;

import graphql.language.Field;
import io.github.graphqly.reflector.annotations.GraphQLEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjector;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorParams;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class EnvironmentInjector implements ArgumentInjector {

  private static final Type listOfFields = new TypeToken<List<Field>>() {}.getType();
  private static final Type setOfStrings = new TypeToken<Set<String>>() {}.getType();

  @Override
  public Object getArgumentValue(ArgumentInjectorParams params) {
    Class raw = GenericTypeReflector.erase(params.getType().getType());
    if (ResolutionEnvironment.class.isAssignableFrom(raw)) {
      return params.getResolutionEnvironment();
    }
    if (GenericTypeReflector.isSuperType(setOfStrings, params.getType().getType())) {
      return params
          .getResolutionEnvironment()
          .dataFetchingEnvironment
          .getSelectionSet()
          .get()
          .keySet();
    }
    if (Field.class.equals(raw)) {
      return params.getResolutionEnvironment().fields.get(0);
    }
    if (GenericTypeReflector.isSuperType(listOfFields, params.getType().getType())) {
      return params.getResolutionEnvironment().fields;
    }
    if (ValueMapper.class.isAssignableFrom(raw)) {
      return params.getResolutionEnvironment().valueMapper;
    }
    throw new IllegalArgumentException(
        "Argument of type "
            + raw.getName()
            + " can not be injected via @"
            + GraphQLEnvironment.class.getSimpleName());
  }

  @Override
  public boolean supports(AnnotatedType type, Parameter parameter) {
    return parameter != null && parameter.isAnnotationPresent(GraphQLEnvironment.class);
  }
}
