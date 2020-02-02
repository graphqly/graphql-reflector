package io.github.graphqly.reflector.module.common.gson;

import graphql.schema.GraphQLScalarType;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class GsonScalarTypeMapper implements TypeMapper {

  @Override
  public GraphQLScalarType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return GsonScalars.toGraphQLScalarType(javaType.getType());
  }

  @Override
  public GraphQLScalarType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return toGraphQLType(javaType, operationMapper, mappersToSkip, buildContext);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return GsonScalars.isScalar(type.getType());
  }
}
