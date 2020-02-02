package io.github.graphqly.reflector.utils.printer;

import graphql.scalars.object.ObjectScalar;
import graphql.schema.*;
import io.github.graphqly.reflector.generator.OperationMapper;
import lombok.Builder;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter
@Builder
public class FunctionPrinter {
  public OperationMapper operationMapper;
  public GraphQLSchema schema;

  public String printMutation(String function) throws Exception {
    StringWriter stringWriter = new StringWriter();
    CodePrinter codePrinter = new CodePrinter(stringWriter);
    GraphQLFieldDefinition field = schema.getMutationType().getFieldDefinition(function);
    if (field == null) {
      throw new Exception("No such function");
    }
    StringBuilder stringBuilder = new StringBuilder();
    describe(FunctionType.MUTATION, function, field.getArguments(), field.getType(), stringBuilder);
    codePrinter.printIndent(stringBuilder.toString());

    return stringWriter.toString();
  }

  private String joinArguments(
      List<GraphQLArgument> arguments, Function<GraphQLArgument, String> mapper) {
    return StringUtils.joinWith(
        ", ", arguments.stream().map(mapper).collect(Collectors.toList()).toArray());
  }

  private String joinFunctionWithArguments(
      String function,
      List<GraphQLArgument> arguments,
      Function<GraphQLArgument, String> argMapper) {
    return function + "(" + joinArguments(arguments, argMapper) + ")";
  }

  private void describe(
      FunctionType type,
      String function,
      List<GraphQLArgument> arguments,
      GraphQLOutputType outputType,
      StringBuilder out) {

    out.append(type.name().toLowerCase() + " ");

    out.append(
        joinFunctionWithArguments(
            function,
            arguments,
            argument -> "$" + argument.getName() + ": " + argument.getType().getName()));

    out.append("{\n");

    out.append(
        joinFunctionWithArguments(
            function, arguments, argument -> argument.getName() + ": $" + argument.getName()));

    describe((GraphQLObjectType) outputType, out);

    out.append("\n}");
  }

  private void describe(GraphQLObjectType type, StringBuilder out) {
    describe("", type, out);
  }

  private void describe(String name, GraphQLObjectType type, StringBuilder out) {
    out.append(StringUtils.isEmpty(name) ? "{" : name + "{");
    for (GraphQLFieldDefinition field : type.getFieldDefinitions()) {
      GraphQLOutputType fieldType = field.getType();
      String fieldName = "\n" + field.getName();

      if (fieldType instanceof GraphQLObjectType) {
        describe(fieldName, (GraphQLObjectType) fieldType, out);
        continue;
      }

      if (fieldType instanceof ObjectScalar) {}

      if (fieldType instanceof GraphQLList) {
        GraphQLType wrappedType = ((GraphQLList) fieldType).getWrappedType();
        if (wrappedType instanceof GraphQLObjectType) {
          describe(fieldName, (GraphQLObjectType) wrappedType, out);
          continue;
        }
      }

      if (operationMapper != null) {
        GraphQLType dynamicType = operationMapper.getDynamicFieldType(type, field);
        if (dynamicType != null) {
          if (dynamicType instanceof GraphQLObjectType) {
            describe(fieldName, (GraphQLObjectType) dynamicType, out);
            continue;
          }
        }
      }

      out.append(fieldName);
    }
    out.append("}");
  }

  public String printQuery(String function) throws Exception {
    StringWriter stringWriter = new StringWriter();
    CodePrinter codePrinter = new CodePrinter(stringWriter);
    GraphQLFieldDefinition field = schema.getQueryType().getFieldDefinition(function);
    if (field == null) {
      throw new Exception("No such function");
    }
    StringBuilder stringBuilder = new StringBuilder();
    describe(FunctionType.QUERY, function, field.getArguments(), field.getType(), stringBuilder);
    codePrinter.printIndent(stringBuilder.toString());

    return stringWriter.toString();
  }

  public enum FunctionType {
    QUERY,
    MUTATION,
    SUBSCRIPTION
  }
}
