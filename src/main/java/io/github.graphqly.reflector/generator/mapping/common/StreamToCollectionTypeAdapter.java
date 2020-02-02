package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.github.graphqly.reflector.generator.mapping.AbstractSimpleTypeAdapter;
import io.github.graphqly.reflector.generator.mapping.DelegatingOutputConverter;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** @author Bojan Tomic (kaqqao) */
public class StreamToCollectionTypeAdapter<T> extends AbstractSimpleTypeAdapter<Stream<T>, List<T>>
    implements DelegatingOutputConverter<Stream<T>, List<T>> {

  @Override
  public List<T> convertOutput(Stream<T> original, AnnotatedType type, ResolutionEnvironment env) {
    try (Stream<T> stream = original) {
      return stream
          .map(item -> env.<T, T>convertOutput(item, env.getDerived(type, 0)))
          .collect(Collectors.toList());
    }
  }

  @Override
  public Stream<T> convertInput(
      List<T> substitute,
      AnnotatedType type,
      GlobalEnvironment environment,
      ValueMapper valueMapper) {
    return substitute.stream()
        .map(item -> environment.convertInput(item, getElementType(type), valueMapper));
  }

  @Override
  public AnnotatedType getSubstituteType(AnnotatedType original) {
    return TypeFactory.parameterizedAnnotatedClass(
        List.class, original.getAnnotations(), getElementType(original));
  }

  private AnnotatedType getElementType(AnnotatedType type) {
    return GenericTypeReflector.getTypeParameter(type, Stream.class.getTypeParameters()[0]);
  }

  @Override
  public List<AnnotatedType> getDerivedTypes(AnnotatedType streamType) {
    return Collections.singletonList(getElementType(streamType));
  }
}
