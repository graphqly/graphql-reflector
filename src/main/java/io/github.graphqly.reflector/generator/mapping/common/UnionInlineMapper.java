package io.github.graphqly.reflector.generator.mapping.common;

import graphql.schema.GraphQLOutputType;
import io.github.graphqly.reflector.annotations.GraphQLUnion;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.generator.union.Union;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** @author Bojan Tomic (kaqqao) */
public class UnionInlineMapper extends UnionMapper {

  @Override
  public GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
    List<AnnotatedType> possibleJavaTypes =
        Arrays.asList(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments());
    return toGraphQLUnion(
        buildContext.interpolate(annotation.name()),
        buildContext.interpolate(annotation.description()),
        javaType,
        possibleJavaTypes,
        operationMapper,
        buildContext);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.isSuperClass(Union.class, type);
  }
}
