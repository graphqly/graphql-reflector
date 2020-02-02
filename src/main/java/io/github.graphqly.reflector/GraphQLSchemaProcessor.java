package io.github.graphqly.reflector;

import graphql.schema.GraphQLSchema;
import io.github.graphqly.reflector.generator.BuildContext;

@FunctionalInterface
public interface GraphQLSchemaProcessor {

  GraphQLSchema.Builder process(GraphQLSchema.Builder schemaBuilder, BuildContext buildContext);
}
