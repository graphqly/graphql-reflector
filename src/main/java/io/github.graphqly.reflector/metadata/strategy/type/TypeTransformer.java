package io.github.graphqly.reflector.metadata.strategy.type;

import io.github.graphqly.reflector.metadata.exceptions.TypeMappingException;

import java.lang.reflect.AnnotatedType;

@FunctionalInterface
public interface TypeTransformer {

  AnnotatedType transform(AnnotatedType type) throws TypeMappingException;
}
