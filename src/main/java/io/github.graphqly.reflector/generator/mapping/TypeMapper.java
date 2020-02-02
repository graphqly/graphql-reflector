package io.github.graphqly.reflector.generator.mapping;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

/**
 * A {@code TypeMapper} is used to map annotated Java types to a GraphQL input or output types,
 * modeled by {@link GraphQLOutputType} and {@link GraphQLInputType} respectively. Method parameter
 * types are mapped to GraphQL input types, while the return types are mapped to GraphQL output
 * types.
 */
public interface TypeMapper {

  GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext);

  GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext);

  boolean supports(AnnotatedType type);
}
