package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.generator.mapping.DelegatingOutputConverter;
import io.github.graphqly.reflector.util.ClassUtils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Only used to trigger the conversion of collection elements */
public class CollectionOutputConverter
    implements DelegatingOutputConverter<Collection<?>, Collection<?>> {

  @Override
  public Collection<?> convertOutput(
      Collection<?> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    return processCollection(
        original, resolutionEnvironment.getDerived(type, 0), resolutionEnvironment);
  }

  @Override
  public List<AnnotatedType> getDerivedTypes(AnnotatedType collectionType) {
    return Collections.singletonList(getElementType(collectionType));
  }

  @Override
  public boolean isTransparent() {
    return true;
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.isSuperClass(Collection.class, type);
  }

  private List<?> processCollection(
      Collection<?> collection,
      AnnotatedType elementType,
      ResolutionEnvironment resolutionEnvironment) {
    return collection.stream()
        .map(e -> resolutionEnvironment.convertOutput(e, elementType))
        .collect(Collectors.toList());
  }

  private AnnotatedType getElementType(AnnotatedType collectionType) {
    return GenericTypeReflector.getTypeParameter(
        collectionType, Collection.class.getTypeParameters()[0]);
  }
}
