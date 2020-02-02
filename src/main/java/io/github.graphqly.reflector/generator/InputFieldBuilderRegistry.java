package io.github.graphqly.reflector.generator;

import io.github.graphqly.reflector.metadata.InputField;
import io.github.graphqly.reflector.metadata.exceptions.MappingException;
import io.github.graphqly.reflector.metadata.strategy.value.InputFieldBuilder;
import io.github.graphqly.reflector.metadata.strategy.value.InputFieldBuilderParams;
import io.github.graphqly.reflector.util.ClassUtils;

import java.util.List;
import java.util.Set;

public class InputFieldBuilderRegistry {

  private final List<InputFieldBuilder> builders;

  public InputFieldBuilderRegistry(List<InputFieldBuilder> builders) {
    this.builders = builders;
  }

  public Set<InputField> getInputFields(InputFieldBuilderParams params) {
    return builders.stream()
        .filter(builder -> builder.supports(params.getType()))
        .findFirst()
        .map(builder -> builder.getInputFields(params))
        .orElseThrow(
            () ->
                new MappingException(
                    String.format(
                        "No %s found for type %s",
                        InputFieldBuilder.class.getSimpleName(),
                        ClassUtils.toString(params.getType()))));
  }
}
