package io.github.graphqly.reflector.generator;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;
import io.github.graphqly.reflector.annotations.GraphQLTypeResolver;
import io.github.graphqly.reflector.metadata.exceptions.UnresolvableTypeException;
import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.Directives;
import io.github.graphqly.reflector.util.Utils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

public class DelegatingTypeResolver implements TypeResolver {

  private final TypeRegistry typeRegistry;
  private final TypeInfoGenerator typeInfoGenerator;
  private final String abstractTypeName;
  private final MessageBundle messageBundle;

  DelegatingTypeResolver(
      TypeRegistry typeRegistry, TypeInfoGenerator typeInfoGenerator, MessageBundle messageBundle) {
    this(null, typeRegistry, typeInfoGenerator, messageBundle);
  }

  DelegatingTypeResolver(
      String abstractTypeName,
      TypeRegistry typeRegistry,
      TypeInfoGenerator typeInfoGenerator,
      MessageBundle messageBundle) {
    this.typeRegistry = typeRegistry;
    this.typeInfoGenerator = typeInfoGenerator;
    this.abstractTypeName = abstractTypeName;
    this.messageBundle = messageBundle;
  }

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    Object result = env.getObject();
    Class<?> resultType = result.getClass();
    String resultTypeName =
        typeInfoGenerator.generateTypeName(
            GenericTypeReflector.annotate(resultType), messageBundle);
    String abstractTypeName =
        this.abstractTypeName != null ? this.abstractTypeName : env.getFieldType().getName();

    // Check if the type is already unambiguous
    List<MappedType> mappedTypes = typeRegistry.getOutputTypes(abstractTypeName, resultType);
    if (mappedTypes.isEmpty()) {
      return (GraphQLObjectType) env.getSchema().getType(resultTypeName);
    }
    if (mappedTypes.size() == 1) {
      return mappedTypes.get(0).getAsObjectType();
    }

    AnnotatedType returnType = Directives.getMappedType(env.getFieldType());
    // Try to find an explicit resolver
    Optional<GraphQLObjectType> resolvedType =
        Utils.or(
                Optional.ofNullable(
                    returnType != null
                        ? returnType.getAnnotation(GraphQLTypeResolver.class)
                        : null),
                Optional.ofNullable(resultType.getAnnotation(GraphQLTypeResolver.class)))
            .map(ann -> resolveType(env, ann));
    if (resolvedType.isPresent()) {
      return resolvedType.get();
    }

    // Try to deduce the type
    if (returnType != null) {
      AnnotatedType resolvedJavaType = GenericTypeReflector.getExactSubType(returnType, resultType);
      if (resolvedJavaType != null
          && !ClassUtils.isMissingTypeParameters(resolvedJavaType.getType())) {
        GraphQLType resolved =
            env.getSchema()
                .getType(typeInfoGenerator.generateTypeName(resolvedJavaType, messageBundle));
        if (resolved == null) {
          throw new UnresolvableTypeException(env.getFieldType().getName(), result);
        }
        return (GraphQLObjectType) resolved;
      }
    }

    // Give up
    throw new UnresolvableTypeException(env.getFieldType().getName(), result);
  }

  private GraphQLObjectType resolveType(
      TypeResolutionEnvironment env, GraphQLTypeResolver descriptor) {
    try {
      return descriptor
          .value()
          .newInstance()
          .resolveType(
              new io.github.graphqly.reflector.execution.TypeResolutionEnvironment(
                  env, typeRegistry, typeInfoGenerator));
    } catch (ReflectiveOperationException e) {
      throw new UnresolvableTypeException(env.<Object>getObject(), e);
    }
  }
}
