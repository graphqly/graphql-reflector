package io.github.graphqly.reflector.metadata.strategy.value;

import io.github.graphqly.reflector.annotations.GraphQLInputField;
import io.github.graphqly.reflector.util.Utils;

import java.lang.reflect.Method;

public class AnnotationMappingUtils {

  public static String inputFieldName(Method method) {
    if (method.isAnnotationPresent(GraphQLInputField.class)) {
      return Utils.coalesce(method.getAnnotation(GraphQLInputField.class).name(), method.getName());
    }
    return method.getName();
  }

  public static String inputFieldDescription(Method method) {
    return method.isAnnotationPresent(GraphQLInputField.class)
        ? method.getAnnotation(GraphQLInputField.class).description()
        : "";
  }
}
