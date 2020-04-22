package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.generator.mapping.AbstractSimpleTypeAdapter;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalDouble;

public class OptionalDoubleAdapter extends AbstractSimpleTypeAdapter<OptionalDouble, Double> {

  @Override
  public Double convertOutput(
      OptionalDouble original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    return original.isPresent() ? original.getAsDouble() : null;
  }

  @Override
  public OptionalDouble convertInput(
      Double substitute,
      AnnotatedType type,
      GlobalEnvironment environment,
      ValueMapper valueMapper) {
    return substitute == null ? OptionalDouble.empty() : OptionalDouble.of(substitute);
  }
}
