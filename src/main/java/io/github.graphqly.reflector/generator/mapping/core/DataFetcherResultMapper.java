package io.github.graphqly.reflector.generator.mapping.core;

import graphql.execution.DataFetcherResult;
import graphql.schema.GraphQLInputType;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.github.graphqly.reflector.util.ClassUtils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class DataFetcherResultMapper extends AbstractTypeSubstitutingMapper {

  @Override
  public GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set mappersToSkip,
      BuildContext buildContext) {
    throw new UnsupportedOperationException(
        DataFetcherResult.class.getSimpleName() + " can not be used as an input type");
  }

  @Override
  public AnnotatedType getSubstituteType(AnnotatedType original) {
    AnnotatedType innerType =
        GenericTypeReflector.getTypeParameter(
            original, DataFetcherResult.class.getTypeParameters()[0]);
    return ClassUtils.addAnnotations(innerType, original.getAnnotations());
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.isSuperClass(DataFetcherResult.class, type);
  }
}
