package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;

/** @author Bojan Tomic (kaqqao) */
public class SuperTypeBasedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

  private final Collection<Class<?>> mappedTypes;

  public SuperTypeBasedInterfaceStrategy(Collection<Class<?>> mappedTypes, boolean mapClasses) {
    super(mapClasses);
    this.mappedTypes = mappedTypes;
  }

  @Override
  public boolean supportsInterface(AnnotatedType interfase) {
    Class<?> raw = ClassUtils.getRawType(interfase.getType());
    return mappedTypes.stream().anyMatch(type -> type.isAssignableFrom(raw));
  }
}
