package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.annotations.GraphQLContext;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorParams;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

/** @author Bojan Tomic (kaqqao) */
public class ContextInjector extends InputValueDeserializer {

  @Override
  public Object getArgumentValue(ArgumentInjectorParams params) {
    return params.getInput() == null
        ? params.getResolutionEnvironment().context
        : super.getArgumentValue(params);
  }

  @Override
  public boolean supports(AnnotatedType type, Parameter parameter) {
    return parameter != null && parameter.isAnnotationPresent(GraphQLContext.class);
  }
}
