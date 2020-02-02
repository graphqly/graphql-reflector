package io.github.graphqly.reflector.metadata.strategy.value;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.annotations.GraphQLInputField;
import io.github.graphqly.reflector.annotations.GraphQLQuery;
import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.util.ReservedStrings;
import io.github.graphqly.reflector.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

public class InputFieldInfoGenerator {

  public Optional<String> getName(List<AnnotatedElement> candidates, MessageBundle messageBundle) {
    Optional<String> explicit =
        candidates.stream()
            .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
            .findFirst()
            .map(element -> element.getAnnotation(GraphQLInputField.class).name());
    Optional<String> implicit =
        candidates.stream()
            .filter(element -> element.isAnnotationPresent(GraphQLQuery.class))
            .findFirst()
            .map(element -> element.getAnnotation(GraphQLQuery.class).name());
    return Utils.or(explicit, implicit).filter(Utils::isNotEmpty).map(messageBundle::interpolate);
  }

  public Optional<String> getDescription(
      List<AnnotatedElement> candidates, MessageBundle messageBundle) {
    Optional<String> explicit =
        candidates.stream()
            .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
            .findFirst()
            .map(element -> element.getAnnotation(GraphQLInputField.class).description());
    Optional<String> implicit =
        candidates.stream()
            .filter(element -> element.isAnnotationPresent(GraphQLQuery.class))
            .findFirst()
            .map(element -> element.getAnnotation(GraphQLQuery.class).description());
    return Utils.or(explicit, implicit).filter(Utils::isNotEmpty).map(messageBundle::interpolate);
  }

  public Optional<Object> defaultValue(
      List<? extends AnnotatedElement> candidates,
      AnnotatedType type,
      GlobalEnvironment environment) {
    return candidates.stream()
        .filter(element -> element.isAnnotationPresent(GraphQLInputField.class))
        .findFirst()
        .map(
            element -> {
              GraphQLInputField ann = element.getAnnotation(GraphQLInputField.class);
              try {
                return defaultValueProvider(ann.defaultValueProvider(), environment)
                    .getDefaultValue(
                        element,
                        type,
                        environment.messageBundle.interpolate(
                            ReservedStrings.decode(ann.defaultValue())));
              } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(
                    ann.defaultValueProvider().getName()
                        + " must expose a public default constructor, or a constructor accepting "
                        + GlobalEnvironment.class.getName(),
                    e);
              }
            });
  }

  @SuppressWarnings("WeakerAccess")
  protected <T extends DefaultValueProvider> T defaultValueProvider(
      Class<T> type, GlobalEnvironment environment) throws ReflectiveOperationException {
    try {
      return type.getConstructor(GlobalEnvironment.class).newInstance(environment);
    } catch (NoSuchMethodException e) {
      return type.getConstructor().newInstance();
    }
  }
}
