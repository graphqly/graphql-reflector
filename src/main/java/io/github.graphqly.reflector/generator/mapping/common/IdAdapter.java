package io.github.graphqly.reflector.generator.mapping.common;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.github.graphqly.reflector.annotations.GraphQLId;
import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.*;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Set;

/** Maps, converts and injects GraphQL IDs */
public class IdAdapter
    implements TypeMapper,
        ArgumentInjector,
        OutputConverter<@GraphQLId Object, String>,
        InputConverter<@GraphQLId Object, String> {

  @Override
  public GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return javaType.getAnnotation(GraphQLId.class).relayId() ? io.github.graphqly.reflector.util.Scalars.RelayId : Scalars.GraphQLID;
  }

  @Override
  public GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return javaType.getAnnotation(GraphQLId.class).relayId() ? io.github.graphqly.reflector.util.Scalars.RelayId : Scalars.GraphQLID;
  }

  @Override
  public String convertOutput(
      Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    if (type.getAnnotation(GraphQLId.class).relayId()) {
      return resolutionEnvironment.globalEnvironment.relay.toGlobalId(
          resolutionEnvironment.parentType.getName(),
          resolutionEnvironment.valueMapper.toString(original, type));
    }
    return resolutionEnvironment.valueMapper.toString(original, type);
  }

  @Override
  public Object convertInput(
      String substitute,
      AnnotatedType type,
      GlobalEnvironment environment,
      ValueMapper valueMapper) {
    String id = substitute;
    if (type.getAnnotation(GraphQLId.class).relayId()) {
      try {
        id = environment.relay.fromGlobalId(id).getId();
      } catch (Exception e) {
        /*no-op*/
      }
    }
    return valueMapper.fromString(id, type);
  }

  @Override
  public Object getArgumentValue(ArgumentInjectorParams params) {
    if (params.getInput() == null) {
      return null;
    }
    ResolutionEnvironment env = params.getResolutionEnvironment();
    return convertInput(
        params.getInput().toString(), params.getType(), env.globalEnvironment, env.valueMapper);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return type.isAnnotationPresent(GraphQLId.class);
  }

  @Override
  public boolean supports(AnnotatedType type, Parameter parameter) {
    return type.isAnnotationPresent(GraphQLId.class)
        || (parameter != null && parameter.isAnnotationPresent(GraphQLId.class));
  }

  @Override
  public AnnotatedType getSubstituteType(AnnotatedType original) {
    return GenericTypeReflector.annotate(String.class);
  }
}
