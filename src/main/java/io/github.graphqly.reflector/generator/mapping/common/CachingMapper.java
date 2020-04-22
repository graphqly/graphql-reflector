package io.github.graphqly.reflector.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;
import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;
import io.github.graphqly.reflector.util.ClassUtils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

/** @author Bojan Tomic (kaqqao) */
public abstract class CachingMapper<
        OutputType extends GraphQLOutputType, InputType extends GraphQLInputType>
    implements TypeMapper {

  @Override
  public GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    String typeName = getTypeName(javaType, buildContext);
    if (buildContext.typeCache.contains(typeName)) {
      return new GraphQLTypeReference(typeName);
    }
    return (GraphQLOutputType)
        buildContext.typeCache.register(
            typeName, () -> toGraphQLType(typeName, javaType, operationMapper, buildContext));
  }

  @Override
  public GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    String typeName = getInputTypeName(javaType, buildContext);
    if (buildContext.typeCache.contains(typeName)) {
      return new GraphQLTypeReference(typeName);
    }
    return (GraphQLInputType)
        buildContext.typeCache.register(
            typeName, () -> toGraphQLInputType(typeName, javaType, operationMapper, buildContext));
  }

  protected abstract OutputType toGraphQLType(
      String typeName,
      AnnotatedType javaType,
      OperationMapper operationMapper,
      BuildContext buildContext);

  protected abstract InputType toGraphQLInputType(
      String typeName,
      AnnotatedType javaType,
      OperationMapper operationMapper,
      BuildContext buildContext);

  protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
    return getTypeName(
        type, getTypeArguments(0), buildContext.typeInfoGenerator, buildContext.messageBundle);
  }

  protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
    return getTypeName(
        type, getTypeArguments(1), buildContext.typeInfoGenerator, buildContext.messageBundle);
  }

  private String getTypeName(
      AnnotatedType javaType,
      AnnotatedType graphQLType,
      TypeInfoGenerator typeInfoGenerator,
      MessageBundle messageBundle) {
    if (ClassUtils.isSuperClass(GraphQLScalarType.class, graphQLType)) {
      return typeInfoGenerator.generateScalarTypeName(javaType, messageBundle);
    }
    if (ClassUtils.isSuperClass(GraphQLEnumType.class, graphQLType)) {
      return typeInfoGenerator.generateTypeName(javaType, messageBundle);
    }
    if (ClassUtils.isSuperClass(GraphQLInputType.class, graphQLType)) {
      return typeInfoGenerator.generateInputTypeName(javaType, messageBundle);
    }
    return typeInfoGenerator.generateTypeName(javaType, messageBundle);
  }

  private AnnotatedType getTypeArguments(int index) {
    return GenericTypeReflector.getTypeParameter(
        getClass().getAnnotatedSuperclass(), CachingMapper.class.getTypeParameters()[index]);
  }
}
