package io.github.graphqly.reflector.metadata.strategy.query;

import graphql.introspection.Introspection;
import io.github.graphqly.reflector.annotations.types.GraphQLDirective;
import io.github.graphqly.reflector.metadata.Directive;
import io.github.graphqly.reflector.metadata.DirectiveArgument;
import io.github.graphqly.reflector.metadata.TypedElement;
import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;
import io.github.graphqly.reflector.metadata.strategy.value.AnnotationMappingUtils;
import io.github.graphqly.reflector.metadata.strategy.value.InputFieldBuilderParams;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.Utils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnnotatedDirectiveBuilder implements DirectiveBuilder {

  @Override
  public List<Directive> buildSchemaDirectives(
      AnnotatedType schemaDescriptorType, DirectiveBuilderParams params) {
    return buildDirectives(schemaDescriptorType, params);
  }

  @Override
  public List<Directive> buildObjectTypeDirectives(
      AnnotatedType type, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getRawType(type.getType()), params);
  }

  @Override
  public List<Directive> buildScalarTypeDirectives(
      AnnotatedType type, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getRawType(type.getType()), params);
  }

  @Override
  public List<Directive> buildFieldDefinitionDirectives(
      AnnotatedElement element, DirectiveBuilderParams params) {
    return buildDirectives(element, params);
  }

  @Override
  public List<Directive> buildArgumentDefinitionDirectives(
      AnnotatedElement element, DirectiveBuilderParams params) {
    return buildDirectives(element, params);
  }

  @Override
  public List<Directive> buildInterfaceTypeDirectives(
      AnnotatedType type, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getRawType(type.getType()), params);
  }

  @Override
  public List<Directive> buildUnionTypeDirectives(
      AnnotatedType type, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getRawType(type.getType()), params);
  }

  @Override
  public List<Directive> buildEnumTypeDirectives(
      AnnotatedType type, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getRawType(type.getType()), params);
  }

  @Override
  public List<Directive> buildEnumValueDirectives(Enum<?> value, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getEnumConstantField(value), params);
  }

  @Override
  public List<Directive> buildInputObjectTypeDirectives(
      AnnotatedType type, DirectiveBuilderParams params) {
    return buildDirectives(ClassUtils.getRawType(type.getType()), params);
  }

  @Override
  public List<Directive> buildInputFieldDefinitionDirectives(
      AnnotatedElement element, DirectiveBuilderParams params) {
    return buildDirectives(element, params);
  }

  @Override
  public Directive buildClientDirective(
      AnnotatedType directiveType, DirectiveBuilderParams params) {
    InputFieldBuilderParams fieldBuilderParams =
        InputFieldBuilderParams.builder()
            .withType(directiveType)
            .withEnvironment(params.getEnvironment())
            .build();
    List<DirectiveArgument> arguments =
        params.getInputFieldBuilders().getInputFields(fieldBuilderParams).stream()
            .map(
                inputField ->
                    new DirectiveArgument(
                        inputField.getName(),
                        inputField.getDescription(),
                        inputField.getTypedElement(),
                        null,
                        inputField.getDefaultValue(),
                        null))
            .collect(Collectors.toList());

    TypeInfoGenerator infoGenerator = params.getEnvironment().typeInfoGenerator;
    MessageBundle messageBundle = params.getEnvironment().messageBundle;
    GraphQLDirective meta = directiveType.getAnnotation(GraphQLDirective.class);
    Introspection.DirectiveLocation[] locations =
        (meta != null && Utils.isArrayNotEmpty(meta.locations()))
            ? meta.locations()
            : GraphQLDirective.ALL_CLIENT;
    return new Directive(
        infoGenerator.generateDirectiveTypeName(directiveType, messageBundle),
        infoGenerator.generateDirectiveTypeDescription(directiveType, messageBundle),
        locations,
        arguments);
  }

  private List<Directive> buildDirectives(AnnotatedElement element, DirectiveBuilderParams params) {
    return Arrays.stream(element.getAnnotations())
        .filter(ann -> ann.annotationType().isAnnotationPresent(GraphQLDirective.class))
        .map(ann -> buildDirective(ann, params))
        .collect(Collectors.toList());
  }

  private Directive buildDirective(Annotation annotation, DirectiveBuilderParams params) {
    GraphQLDirective meta = annotation.annotationType().getAnnotation(GraphQLDirective.class);
    List<DirectiveArgument> arguments =
        ClassUtils.getAnnotationFields(annotation.annotationType()).stream()
            .map(method -> buildDirectiveArgument(annotation, method))
            .collect(Collectors.toList());
    Introspection.DirectiveLocation[] locations =
        Utils.isArrayNotEmpty(meta.locations()) ? meta.locations() : GraphQLDirective.ALL_SCHEMA;
    TypeInfoGenerator infoGenerator = params.getEnvironment().typeInfoGenerator;
    MessageBundle messageBundle = params.getEnvironment().messageBundle;
    AnnotatedType directiveType = GenericTypeReflector.annotate(annotation.annotationType());
    return new Directive(
        infoGenerator.generateDirectiveTypeName(directiveType, messageBundle),
        infoGenerator.generateDirectiveTypeDescription(directiveType, messageBundle),
        locations,
        arguments);
  }

  private DirectiveArgument buildDirectiveArgument(Annotation annotation, Method method) {
    try {
      TypedElement element =
          new TypedElement(GenericTypeReflector.annotate(method.getReturnType()), method);
      return new DirectiveArgument(
          AnnotationMappingUtils.inputFieldName(method),
          AnnotationMappingUtils.inputFieldDescription(method),
          element,
          method.invoke(annotation),
          method.getDefaultValue(),
          annotation);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
