package io.github.graphqly.reflector.execution;

import io.github.graphqly.reflector.generator.TypeRegistry;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;

public class TypeResolutionEnvironment extends graphql.TypeResolutionEnvironment {

  private final TypeRegistry typeRegistry;
  private final TypeInfoGenerator typeInfoGenerator;

  public TypeResolutionEnvironment(
      graphql.TypeResolutionEnvironment environment,
      TypeRegistry typeRegistry,
      TypeInfoGenerator typeInfoGenerator) {
    super(
        environment.getObject(),
        environment.getArguments(),
        environment.getField(),
        environment.getFieldType(),
        environment.getSchema(),
        environment.getContext());
    this.typeRegistry = typeRegistry;
    this.typeInfoGenerator = typeInfoGenerator;
  }

  public TypeRegistry getTypeRegistry() {
    return typeRegistry;
  }

  public TypeInfoGenerator getTypeInfoGenerator() {
    return typeInfoGenerator;
  }
}
