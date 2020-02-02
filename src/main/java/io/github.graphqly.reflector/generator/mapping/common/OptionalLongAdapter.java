package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.github.graphqly.reflector.generator.mapping.AbstractSimpleTypeAdapter;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalLong;

public class OptionalLongAdapter extends AbstractSimpleTypeAdapter<OptionalLong, Long> {

  @Override
  public Long convertOutput(
      OptionalLong original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    return original.isPresent() ? original.getAsLong() : null;
  }

  @Override
  public OptionalLong convertInput(
          Long substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
    return substitute == null ? OptionalLong.empty() : OptionalLong.of(substitute);
  }
}
