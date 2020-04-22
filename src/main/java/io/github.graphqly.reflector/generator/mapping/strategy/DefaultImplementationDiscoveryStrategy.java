package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.classgraph.ClassInfo;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.metadata.exceptions.TypeMappingException;
import io.github.graphqly.reflector.util.ClassFinder;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.Utils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultImplementationDiscoveryStrategy implements ImplementationDiscoveryStrategy {

  private final List<Predicate<ClassInfo>> filters;
  private final List<Class<?>> additionalImplementations;

  public DefaultImplementationDiscoveryStrategy() {
    this.filters = new ArrayList<>();
    this.filters.add(ClassFinder.PUBLIC);
    this.additionalImplementations = new ArrayList<>();
  }

  @Override
  public List<AnnotatedType> findImplementations(
      AnnotatedType type, boolean autoDiscover, String[] scanPackages, BuildContext buildContext) {
    if (Utils.isArrayEmpty(scanPackages) && Utils.isArrayNotEmpty(buildContext.basePackages)) {
      scanPackages = buildContext.basePackages;
    }
    Predicate<ClassInfo> filter =
        ClassFinder.NON_IGNORED.and(
            filters.stream().reduce(Predicate::and).orElse(ClassFinder.ALL));

    List<AnnotatedType> additionalImpls = additionalImplementationsOf(type);
    if (!autoDiscover) {
      return additionalImpls;
    }
    List<AnnotatedType> discoveredImpls =
        buildContext.classFinder.findImplementations(type, filter, false, scanPackages);
    Set<Class<?>> seen = new HashSet<>(discoveredImpls.size() + additionalImpls.size());
    return Stream.concat(additionalImpls.stream(), discoveredImpls.stream())
        .filter(impl -> seen.add(GenericTypeReflector.erase(impl.getType())))
        .collect(Collectors.toList());
  }

  public DefaultImplementationDiscoveryStrategy withNonPublicClasses() {
    this.filters.remove(ClassFinder.PUBLIC);
    return this;
  }

  @SafeVarargs
  public final DefaultImplementationDiscoveryStrategy withFilters(Predicate<ClassInfo>... filters) {
    Collections.addAll(this.filters, filters);
    return this;
  }

  public DefaultImplementationDiscoveryStrategy withAdditionalImplementations(
      Class<?>... additionalImplementations) {
    Collections.addAll(this.additionalImplementations, additionalImplementations);
    return this;
  }

  private List<AnnotatedType> additionalImplementationsOf(AnnotatedType type) {
    return additionalImplementations.stream()
        .filter(impl -> ClassUtils.isSuperClass(type, impl))
        .map(
            impl -> {
              AnnotatedType implType = GenericTypeReflector.getExactSubType(type, impl);
              if (implType == null || ClassUtils.isMissingTypeParameters(implType.getType())) {
                throw new TypeMappingException(
                    String.format(
                        "%s could not be resolved as a subtype of %s",
                        impl.getName(), type.getType().getTypeName()));
              }
              return implType;
            })
        .collect(Collectors.toList());
  }
}
