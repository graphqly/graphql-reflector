package io.github.graphqly.reflector.generator.mapping;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.metadata.Directive;
import io.github.graphqly.reflector.metadata.DirectiveArgument;
import io.github.graphqly.reflector.metadata.InputField;
import io.github.graphqly.reflector.metadata.Operation;
import io.github.graphqly.reflector.metadata.OperationArgument;

public interface SchemaTransformer {

  default GraphQLFieldDefinition transformField(
      GraphQLFieldDefinition field,
      Operation operation,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return field;
  }

  default GraphQLInputObjectField transformInputField(
      GraphQLInputObjectField field,
      InputField inputField,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return field;
  }

  default GraphQLArgument transformArgument(
      GraphQLArgument argument,
      OperationArgument operationArgument,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return argument;
  }

  default GraphQLArgument transformArgument(
      GraphQLArgument argument,
      DirectiveArgument directiveArgument,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return argument;
  }

  default GraphQLDirective transformDirective(
      GraphQLDirective directive,
      Directive directiveModel,
      OperationMapper operationMapper,
      BuildContext buildContext) {
    return directive;
  }
}
