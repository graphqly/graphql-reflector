package io.github.graphqly.reflector.metadata.strategy.query;

import io.github.graphqly.reflector.annotations.GraphQLArgument;
import io.github.graphqly.reflector.annotations.GraphQLContext;
import io.github.graphqly.reflector.annotations.GraphQLId;
import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.metadata.OperationArgument;
import io.github.graphqly.reflector.metadata.exceptions.TypeMappingException;
import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.metadata.strategy.InclusionStrategy;
import io.github.graphqly.reflector.metadata.strategy.value.DefaultValueProvider;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.ReservedStrings;
import io.github.graphqly.reflector.util.Urls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class AnnotatedArgumentBuilder implements ResolverArgumentBuilder {

  private static final Logger log = LoggerFactory.getLogger(AnnotatedArgumentBuilder.class);

  @Override
  public List<OperationArgument> buildResolverArguments(ArgumentBuilderParams params) {
    Method resolverMethod = params.getResolverMethod();
    List<OperationArgument> operationArguments =
        new ArrayList<>(resolverMethod.getParameterCount());
    AnnotatedType[] parameterTypes =
        ClassUtils.getParameterTypes(resolverMethod, params.getDeclaringType());
    for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
      Parameter parameter = resolverMethod.getParameters()[i];
      if (parameter.isSynthetic() || parameter.isImplicit()) continue;
      AnnotatedType parameterType;
      try {
        parameterType = params.getTypeTransformer().transform(parameterTypes[i]);
      } catch (TypeMappingException e) {
        throw new TypeMappingException(resolverMethod, parameter, e);
      }
      operationArguments.add(
          buildResolverArgument(
              parameter, parameterType, params.getInclusionStrategy(), params.getEnvironment()));
    }
    return operationArguments;
  }

  protected OperationArgument buildResolverArgument(
      Parameter parameter,
      AnnotatedType parameterType,
      InclusionStrategy inclusionStrategy,
      GlobalEnvironment environment) {
    return new OperationArgument(
        parameterType,
        getArgumentName(parameter, parameterType, inclusionStrategy, environment.messageBundle),
        getArgumentDescription(parameter, parameterType, environment.messageBundle),
        defaultValue(parameter, parameterType, environment),
        parameter,
        parameter.isAnnotationPresent(GraphQLContext.class),
        inclusionStrategy.includeArgument(parameter, parameterType));
  }

  protected String getArgumentName(
      Parameter parameter,
      AnnotatedType parameterType,
      InclusionStrategy inclusionStrategy,
      MessageBundle messageBundle) {
    if (Optional.ofNullable(parameterType.getAnnotation(GraphQLId.class))
        .filter(GraphQLId::relayId)
        .isPresent()) {
      return GraphQLId.RELAY_ID_FIELD_NAME;
    }
    GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
    if (meta != null && !meta.name().isEmpty()) {
      return messageBundle.interpolate(meta.name());
    } else {
      if (!parameter.isNamePresent()
          && inclusionStrategy.includeArgument(parameter, parameterType)) {
        log.warn(
            "No explicit argument name given and the parameter name lost in compilation: "
                + parameter.getDeclaringExecutable().toGenericString()
                + "#"
                + parameter.toString()
                + ". For details and possible solutions see "
                + Urls.Errors.MISSING_ARGUMENT_NAME);
      }
      return parameter.getName();
    }
  }

  protected String getArgumentDescription(
      Parameter parameter, AnnotatedType parameterType, MessageBundle messageBundle) {
    GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
    return meta != null ? messageBundle.interpolate(meta.description()) : null;
  }

  protected Object defaultValue(
      Parameter parameter, AnnotatedType parameterType, GlobalEnvironment environment) {

    GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
    if (meta == null) return null;
    try {
      return defaultValueProvider(meta.defaultValueProvider(), environment)
          .getDefaultValue(
              parameter,
              environment.getMappableInputType(parameterType),
              ReservedStrings.decode(environment.messageBundle.interpolate(meta.defaultValue())));
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(
          meta.defaultValueProvider().getName()
              + " must expose a public default constructor, or a constructor accepting "
              + GlobalEnvironment.class.getName(),
          e);
    }
  }

  protected <T extends DefaultValueProvider> T defaultValueProvider(
      Class<T> type, GlobalEnvironment environment) throws ReflectiveOperationException {
    try {
      return type.getConstructor(GlobalEnvironment.class).newInstance(environment);
    } catch (NoSuchMethodException e) {
      return type.getConstructor().newInstance();
    }
  }
}
