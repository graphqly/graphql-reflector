package io.github.graphqly.reflector.metadata.strategy.query;

import io.github.graphqly.reflector.annotations.GraphQLMutation;
import io.github.graphqly.reflector.annotations.GraphQLQuery;
import io.github.graphqly.reflector.annotations.GraphQLSubscription;
import io.github.graphqly.reflector.metadata.TypedElement;
import io.github.graphqly.reflector.metadata.exceptions.TypeMappingException;
import io.github.graphqly.reflector.metadata.strategy.value.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A resolver builder that exposes only the methods explicitly annotated with {@link GraphQLQuery}
 */
public class AnnotatedResolverBuilder extends PublicResolverBuilder {

  public AnnotatedResolverBuilder() {
    this.operationInfoGenerator = new DefaultOperationInfoGenerator();
    this.argumentBuilder = new AnnotatedArgumentBuilder();
    this.propertyElementReducer = AnnotatedResolverBuilder::annotatedElementReducer;
    withDefaultFilters();
  }

  private static TypedElement annotatedElementReducer(TypedElement field, TypedElement getter) {
    if (field.isAnnotationPresent(GraphQLQuery.class)
        && getter.isAnnotationPresent(GraphQLQuery.class)) {
      throw new TypeMappingException("Ambiguous mapping of " + field);
    }
    return field.isAnnotationPresent(GraphQLQuery.class) ? field : getter;
  }

  @Override
  protected boolean isQuery(Method method, ResolverBuilderParams params) {
    return method.isAnnotationPresent(GraphQLQuery.class);
  }

  @Override
  protected boolean isQuery(Field field, ResolverBuilderParams params) {
    return field.isAnnotationPresent(GraphQLQuery.class);
  }

  @Override
  protected boolean isQuery(Property property, ResolverBuilderParams params) {
    return isQuery(property.getGetter(), params) || isQuery(property.getField(), params);
  }

  @Override
  protected boolean isMutation(Method method, ResolverBuilderParams params) {
    return method.isAnnotationPresent(GraphQLMutation.class);
  }

  @Override
  protected boolean isSubscription(Method method, ResolverBuilderParams params) {
    return method.isAnnotationPresent(GraphQLSubscription.class);
  }
}
