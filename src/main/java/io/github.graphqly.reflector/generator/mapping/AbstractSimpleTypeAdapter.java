package io.github.graphqly.reflector.generator.mapping;

import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

public abstract class AbstractSimpleTypeAdapter<T, S> extends AbstractTypeAdapter<T, S> {

  @SuppressWarnings("WeakerAccess")
  protected final Class<?> rawSourceType;

  protected AbstractSimpleTypeAdapter() {
    this.rawSourceType = ClassUtils.getRawType(sourceType.getType());
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.isSuperClass(rawSourceType, type);
  }
}
