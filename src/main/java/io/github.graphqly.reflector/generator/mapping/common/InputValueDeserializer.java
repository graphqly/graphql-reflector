package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.generator.mapping.ArgumentInjector;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorParams;
import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** @author Bojan Tomic (kaqqao) */
public class InputValueDeserializer implements ArgumentInjector {

  private static final Map<Class<?>, Object> EMPTY_VALUES = emptyValues();

  private static Map<Class<?>, Object> emptyValues() {
    Map<Class<?>, Object> empty = new HashMap<>();
    empty.put(Optional.class, Optional.empty());
    empty.put(OptionalInt.class, OptionalInt.empty());
    empty.put(OptionalLong.class, OptionalLong.empty());
    empty.put(OptionalDouble.class, OptionalDouble.empty());
    return Collections.unmodifiableMap(empty);
  }

  @Override
  public Object getArgumentValue(ArgumentInjectorParams params) {
    if (params.getInput() == null) {
      if (params.isPresent()) {
        return EMPTY_VALUES.getOrDefault(ClassUtils.getRawType(params.getType().getType()), null);
      }
      return null;
    }
    return params
        .getResolutionEnvironment()
        .valueMapper
        .fromInput(params.getInput(), params.getType());
  }

  @Override
  public boolean supports(AnnotatedType type, Parameter parameter) {
    return true;
  }
}
