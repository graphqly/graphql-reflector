package io.github.graphqly.reflector.generator.mapping.common;

import graphql.schema.GraphQLScalarType;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.util.Scalars;

import java.lang.reflect.AnnotatedType;

/** @author Bojan Tomic (kaqqao) */
public class ScalarMapper extends CachingMapper<GraphQLScalarType, GraphQLScalarType> {

  @Override
  public GraphQLScalarType toGraphQLType(
      String typeName,
      AnnotatedType javaType,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return Scalars.toGraphQLScalarType(javaType.getType());
  }

  @Override
  public GraphQLScalarType toGraphQLInputType(
      String typeName,
      AnnotatedType javaType,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return toGraphQLType(typeName, javaType, operationMapper, buildContext);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return Scalars.isScalar(type.getType());
  }

  @Override
  protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
    return Scalars.toGraphQLScalarType(type.getType()).getName();
  }

  @Override
  protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
    return getTypeName(type, buildContext);
  }
}
