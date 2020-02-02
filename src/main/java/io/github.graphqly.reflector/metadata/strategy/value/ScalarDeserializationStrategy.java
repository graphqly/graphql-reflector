package io.github.graphqly.reflector.metadata.strategy.value;

import java.lang.reflect.AnnotatedType;

public interface ScalarDeserializationStrategy {

  boolean isDirectlyDeserializable(AnnotatedType type);
}
