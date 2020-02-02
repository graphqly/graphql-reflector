package io.github.graphqly.reflector.metadata.strategy.value;

import io.leangen.geantyref.GenericTypeReflector;
import io.github.graphqly.reflector.metadata.InputField;
import io.github.graphqly.reflector.metadata.TypedElement;
import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationInputFieldBuilder implements InputFieldBuilder {

  @Override
  public Set<InputField> getInputFields(InputFieldBuilderParams params) {
    return ClassUtils.getAnnotationFields(ClassUtils.getRawType(params.getType().getType()))
        .stream()
        .map(
            method ->
                new InputField(
                    AnnotationMappingUtils.inputFieldName(method),
                    AnnotationMappingUtils.inputFieldDescription(method),
                    new TypedElement(GenericTypeReflector.annotate(method.getReturnType()), method),
                    null,
                    method.getDefaultValue()))
        .collect(Collectors.toSet());
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.getRawType(type.getType()).isAnnotation();
  }
}
