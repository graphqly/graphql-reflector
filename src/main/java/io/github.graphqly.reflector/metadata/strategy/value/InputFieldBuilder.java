package io.github.graphqly.reflector.metadata.strategy.value;

import io.github.graphqly.reflector.metadata.InputField;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public interface InputFieldBuilder {

  Set<InputField> getInputFields(InputFieldBuilderParams params);

  boolean supports(AnnotatedType type);
}
