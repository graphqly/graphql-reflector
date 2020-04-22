package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.annotations.GraphQLRootContext;
import io.github.graphqly.reflector.execution.ContextWrapper;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjector;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorParams;
import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Map;

/** @author Bojan Tomic (kaqqao) */
public class RootContextInjector implements ArgumentInjector {

  @Override
  public Object getArgumentValue(ArgumentInjectorParams params) {
    String injectionExpression =
        params.getParameter().getAnnotation(GraphQLRootContext.class).value();
    ResolutionEnvironment env = params.getResolutionEnvironment();
    Object rootContext =
        env.rootContext instanceof ContextWrapper
            ? ((ContextWrapper) env.rootContext).getContext()
            : env.rootContext;
    return injectionExpression.isEmpty() ? rootContext : extract(rootContext, injectionExpression);
  }

  @Override
  public boolean supports(AnnotatedType type, Parameter parameter) {
    return parameter != null && parameter.isAnnotationPresent(GraphQLRootContext.class);
  }

  private Object extract(Object input, String expression) {
    if (input instanceof Map) {
      return ((Map) input).get(expression);
    }
    return ClassUtils.getFieldValue(input, expression);
  }
}
