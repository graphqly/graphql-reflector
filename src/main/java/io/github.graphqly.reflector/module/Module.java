package io.github.graphqly.reflector.module;

import io.github.graphqly.reflector.GraphQLSchemaGenerator;

public interface Module {

  void setUp(SetupContext context);

  interface SetupContext {
    GraphQLSchemaGenerator getSchemaGenerator();
  }
}
