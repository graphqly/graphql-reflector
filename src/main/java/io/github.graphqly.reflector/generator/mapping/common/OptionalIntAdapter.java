package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.github.graphqly.reflector.generator.mapping.AbstractSimpleTypeAdapter;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalInt;

public class OptionalIntAdapter extends AbstractSimpleTypeAdapter<OptionalInt, Integer> {

  @Override
  public Integer convertOutput(
      OptionalInt original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    return original.isPresent() ? original.getAsInt() : null;
  }

  @Override
  public OptionalInt convertInput(
      Integer substitute,
      AnnotatedType type,
      GlobalEnvironment environment,
      ValueMapper valueMapper) {
    return substitute == null ? OptionalInt.empty() : OptionalInt.of(substitute);
  }
}
