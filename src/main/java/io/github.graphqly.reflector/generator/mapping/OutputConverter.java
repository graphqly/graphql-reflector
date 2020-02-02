package io.github.graphqly.reflector.generator.mapping;

import io.github.graphqly.reflector.execution.ResolutionEnvironment;

import java.lang.reflect.AnnotatedType;

/** @author Bojan Tomic (kaqqao) */
public interface OutputConverter<T, S> {

  S convertOutput(T original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment);

  boolean supports(AnnotatedType type);
}
