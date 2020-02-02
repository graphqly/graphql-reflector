package io.github.graphqly.reflector.utils.printer;

import graphql.schema.GraphQLSchema;
import io.github.graphqly.reflector.generator.OperationMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

class BlueprintWriter extends PrintWriter {
  public final OperationMapper operationMapper;

  public BlueprintWriter(Writer out, OperationMapper operationMapper) {
    super(out);
    this.operationMapper = operationMapper;
  }

  public static BlueprintWriter getDefault(
      StringWriter sw, GraphQLSchema schema, OperationMapper buildContext) {
    return new BlueprintWriter(sw, buildContext);
  }

  public static BlueprintWriter getDefault(StringWriter sw, GraphQLSchema schema) {
    return getDefault(sw, schema, null);
  }
}
