package io.github.graphqly.reflector.module.common.jackson;

import com.fasterxml.jackson.databind.node.POJONode;
import graphql.schema.GraphQLScalarType;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class JacksonObjectScalarMapper implements TypeMapper {

  @Override
  public GraphQLScalarType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return JacksonObjectScalars.toGraphQLScalarType(javaType.getType());
  }

  @Override
  public GraphQLScalarType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    if (POJONode.class.equals(javaType.getType())) {
      throw new UnsupportedOperationException(
          POJONode.class.getSimpleName() + " can not be used as input");
    }
    return toGraphQLType(javaType, operationMapper, mappersToSkip, buildContext);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return JacksonObjectScalars.isScalar(type.getType());
  }
}
