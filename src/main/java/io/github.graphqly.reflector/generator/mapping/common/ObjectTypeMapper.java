package io.github.graphqly.reflector.generator.mapping.common;

import graphql.schema.*;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.metadata.strategy.value.InputFieldBuilderParams;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.github.graphqly.reflector.util.ClassUtils;
import io.github.graphqly.reflector.util.Directives;
import io.leangen.geantyref.GenericTypeReflector;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

  @Override
  public GraphQLObjectType toGraphQLType(
      String typeName,
      AnnotatedType javaType,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    GraphQLObjectType.Builder typeBuilder =
        newObject()
            .name(typeName)
            .description(
                buildContext.typeInfoGenerator.generateTypeDescription(
                    javaType, buildContext.messageBundle));

    List<GraphQLFieldDefinition> fields =
        getFields(typeName, javaType, buildContext, operationMapper);
    fields.forEach(typeBuilder::field);

    List<GraphQLOutputType> interfaces =
        getInterfaces(javaType, fields, buildContext, operationMapper);
    interfaces.forEach(
        inter -> {
          if (inter instanceof GraphQLInterfaceType) {
            typeBuilder.withInterface((GraphQLInterfaceType) inter);
          } else {
            typeBuilder.withInterface((GraphQLTypeReference) inter);
          }
        });

    typeBuilder.withDirective(Directives.mappedType(javaType));
    buildContext
        .directiveBuilder
        .buildObjectTypeDirectives(javaType, buildContext.directiveBuilderParams())
        .forEach(
            directive ->
                typeBuilder.withDirective(
                    operationMapper.toGraphQLDirective(directive, buildContext)));

    GraphQLObjectType type = typeBuilder.build();
    interfaces.forEach(
        inter -> buildContext.typeRegistry.registerCovariantType(inter.getName(), javaType, type));
    return type;
  }

  @Override
  public GraphQLInputObjectType toGraphQLInputType(
      String typeName,
      AnnotatedType javaType,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    GraphQLInputObjectType.Builder typeBuilder =
        newInputObject()
            .name(typeName)
            .description(
                buildContext.typeInfoGenerator.generateInputTypeDescription(
                    javaType, buildContext.messageBundle));

    InputFieldBuilderParams params =
        InputFieldBuilderParams.builder()
            .withType(javaType)
            .withEnvironment(buildContext.globalEnvironment)
            .build();
    buildContext
        .inputFieldBuilders
        .getInputFields(params)
        .forEach(
            field -> typeBuilder.field(operationMapper.toGraphQLInputField(field, buildContext)));
    if (ClassUtils.isAbstract(javaType)) {
      createInputDisambiguatorField(javaType, buildContext).ifPresent(typeBuilder::field);
    }

    typeBuilder.withDirective(Directives.mappedType(javaType));
    buildContext
        .directiveBuilder
        .buildInputObjectTypeDirectives(javaType, buildContext.directiveBuilderParams())
        .forEach(
            directive ->
                typeBuilder.withDirective(
                    operationMapper.toGraphQLDirective(directive, buildContext)));

    return typeBuilder.build();
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return true;
  }

  @SuppressWarnings("WeakerAccess")
  protected List<GraphQLFieldDefinition> getFields(
      String typeName,
      AnnotatedType javaType,
      BuildContext buildContext,
      OperationMapper operationMapper) {
    return buildContext.operationRegistry.getChildQueries(javaType).stream()
        .map(childQuery -> operationMapper.toGraphQLField(typeName, childQuery, buildContext))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("WeakerAccess")
  protected List<GraphQLOutputType> getInterfaces(
      AnnotatedType javaType,
      List<GraphQLFieldDefinition> fields,
      BuildContext buildContext,
      OperationMapper operationMapper) {

    List<GraphQLOutputType> interfaces = new ArrayList<>();
    if (buildContext.relayMappingConfig.inferNodeInterface
        && fields.stream().anyMatch(GraphQLUtils::isRelayId)) {
      interfaces.add(buildContext.node);
    }
    buildContext
        .interfaceStrategy
        .getInterfaces(javaType)
        .forEach(inter -> interfaces.add(operationMapper.toGraphQLType(inter, buildContext)));

    return interfaces;
  }

  @SuppressWarnings("WeakerAccess")
  protected Optional<GraphQLInputObjectField> createInputDisambiguatorField(
      AnnotatedType javaType, BuildContext buildContext) {
    Class<?> raw = ClassUtils.getRawType(javaType.getType());
    String typeName =
        buildContext.typeInfoGenerator.generateTypeName(
                GenericTypeReflector.annotate(raw), buildContext.messageBundle)
            + "TypeDisambiguator";
    GraphQLInputType fieldType = null;
    if (buildContext.typeCache.contains(typeName)) {
      fieldType = new GraphQLTypeReference(typeName);
    } else {
      List<AnnotatedType> impls =
          buildContext.abstractInputHandler.findConcreteSubTypes(raw, buildContext).stream()
              .map(GenericTypeReflector::annotate)
              .collect(Collectors.toList());
      if (impls.size() > 1) {
        buildContext.typeCache.register(typeName);
        GraphQLEnumType.Builder builder =
            GraphQLEnumType.newEnum().name(typeName).description("Input type discriminator");
        impls.stream()
            .map(
                t -> buildContext.typeInfoGenerator.generateTypeName(t, buildContext.messageBundle))
            .forEach(builder::value);
        fieldType = builder.build();
      }
    }
    return Optional.ofNullable(fieldType)
        .map(
            type ->
                newInputObjectField()
                    .name(ValueMapper.TYPE_METADATA_FIELD_NAME)
                    .type(type)
                    .build());
  }
}
