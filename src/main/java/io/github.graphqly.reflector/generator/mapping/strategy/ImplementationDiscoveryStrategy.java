package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.graphqly.reflector.generator.BuildContext;

import java.lang.reflect.AnnotatedType;
import java.util.List;

public interface ImplementationDiscoveryStrategy {

  List<AnnotatedType> findImplementations(
      AnnotatedType type, boolean autoDiscover, String[] scanPackages, BuildContext buildContext);
}
