package io.github.graphqly.reflector.generator.mapping;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.metadata.*;

import java.util.Collections;
import java.util.List;

public class SchemaTransformerRegistry {

  private final List<SchemaTransformer> transformers;

  public SchemaTransformerRegistry(List<SchemaTransformer> transformers) {
    this.transformers = Collections.unmodifiableList(transformers);
  }

  public GraphQLFieldDefinition transform(
      GraphQLFieldDefinition field,
      Operation operation,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    for (SchemaTransformer transformer : transformers) {
      field = transformer.transformField(field, operation, operationMapper, buildContext);
    }
    return field;
  }

  public GraphQLInputObjectField transform(
      GraphQLInputObjectField field,
      InputField inputField,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    for (SchemaTransformer transformer : transformers) {
      field = transformer.transformInputField(field, inputField, operationMapper, buildContext);
    }
    return field;
  }

  public GraphQLArgument transform(
      GraphQLArgument argument,
      OperationArgument operationArgument,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    for (SchemaTransformer transformer : transformers) {
      argument =
          transformer.transformArgument(argument, operationArgument, operationMapper, buildContext);
    }
    return argument;
  }

  public GraphQLArgument transform(
      GraphQLArgument argument,
      DirectiveArgument directiveArgument,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    for (SchemaTransformer transformer : transformers) {
      argument =
          transformer.transformArgument(argument, directiveArgument, operationMapper, buildContext);
    }
    return argument;
  }

  public GraphQLDirective transform(
      GraphQLDirective directive,
      Directive directiveModel,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    for (SchemaTransformer transformer : transformers) {
      directive =
          transformer.transformDirective(directive, directiveModel, operationMapper, buildContext);
    }
    return directive;
  }
}
