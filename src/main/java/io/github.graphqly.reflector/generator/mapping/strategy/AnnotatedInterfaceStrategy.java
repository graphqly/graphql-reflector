package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.graphqly.reflector.annotations.types.GraphQLInterface;

import java.lang.reflect.AnnotatedType;

/** @author Bojan Tomic (kaqqao) */
public class AnnotatedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

  public AnnotatedInterfaceStrategy(boolean mapClasses) {
    super(mapClasses);
  }

  @Override
  public boolean supportsInterface(AnnotatedType interfase) {
    return interfase.isAnnotationPresent(GraphQLInterface.class);
  }
}
