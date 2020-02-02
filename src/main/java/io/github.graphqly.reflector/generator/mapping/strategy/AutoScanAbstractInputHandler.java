package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.metadata.exceptions.TypeMappingException;
import io.github.graphqly.reflector.metadata.strategy.value.InputFieldBuilderParams;
import io.github.graphqly.reflector.util.ClassFinder;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.Scalars;
import io.github.classgraph.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AutoScanAbstractInputHandler implements AbstractInputHandler {

  private static final Logger log = LoggerFactory.getLogger(AutoScanAbstractInputHandler.class);
  private final Map<Type, Set<Type>> abstractComponents = new HashMap<>();
  private final List<Predicate<ClassInfo>> filters;

  public AutoScanAbstractInputHandler() {
    this.filters = new ArrayList<>();
    this.filters.add(ClassFinder.PUBLIC);
  }

  @Override
  public Set<Type> findConstituentAbstractTypes(AnnotatedType javaType, BuildContext buildContext) {
    if (Scalars.isScalar(javaType.getType())
        || ClassUtils.isSubPackage(ClassUtils.getRawType(javaType.getType()).getPackage(), "java.")
        || buildContext.scalarStrategy.isDirectlyDeserializable(javaType)) {
      return Collections.emptySet();
    }
    if (javaType instanceof AnnotatedParameterizedType) {
      Set<Type> abstractTypes =
          Arrays.stream(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments())
              .flatMap(arg -> findConstituentAbstractTypes(arg, buildContext).stream())
              .collect(Collectors.toSet());
      abstractTypes.addAll(findAbstract(javaType, buildContext));
      return abstractTypes;
    }
    if (javaType instanceof AnnotatedArrayType) {
      return findConstituentAbstractTypes(
          ((AnnotatedArrayType) javaType).getAnnotatedGenericComponentType(), buildContext);
    }
    if (javaType instanceof AnnotatedWildcardType || javaType instanceof AnnotatedTypeVariable) {
      throw new TypeMappingException(javaType.getType());
    }
    return findAbstract(javaType, buildContext);
  }

  @Override
  public List<Class<?>> findConcreteSubTypes(Class abstractType, BuildContext buildContext) {
    Predicate<ClassInfo> filter =
        ClassFinder.CONCRETE.and(ClassFinder.NON_IGNORED).and(filters.stream().reduce(Predicate::and).orElse(ClassFinder.ALL));
    List<Class<?>> subTypes =
        buildContext.classFinder.findImplementations(
            abstractType, filter, buildContext.basePackages);
    if (subTypes.isEmpty()) {
      log.warn("No concrete subtypes of " + abstractType.getName() + " found");
    }
    return subTypes;
  }

  public AutoScanAbstractInputHandler withNonPublicClasses() {
    this.filters.remove(ClassFinder.PUBLIC);
    return this;
  }

  @SafeVarargs
  public final AutoScanAbstractInputHandler withFilters(Predicate<ClassInfo>... filters) {
    Collections.addAll(this.filters, filters);
    return this;
  }

  private Set<Type> findAbstract(AnnotatedType javaType, BuildContext buildContext) {
    if (abstractComponents.get(javaType.getType()) != null) {
      return abstractComponents.get(javaType.getType());
    }
    if (abstractComponents.containsKey(javaType.getType())) {
      return Collections.emptySet();
    }
    abstractComponents.put(javaType.getType(), null);
    Set<Type> abstractTypes = new HashSet<>();
    if (ClassUtils.isAbstract(javaType)) {
      abstractTypes.add(javaType.getType());
    }
    buildContext
        .inputFieldBuilders
        .getInputFields(
            InputFieldBuilderParams.builder()
                .withType(javaType)
                .withEnvironment(buildContext.globalEnvironment)
                .build())
        .forEach(
            childQuery ->
                abstractTypes.addAll(
                    findConstituentAbstractTypes(
                        childQuery.getDeserializableType(), buildContext)));
    abstractComponents.put(javaType.getType(), abstractTypes);
    return abstractTypes;
  }
}
