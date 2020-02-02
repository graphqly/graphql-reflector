package io.github.graphqly.reflector;

import io.github.graphqly.reflector.execution.GlobalEnvironment;

public class ExtendedGeneratorConfiguration extends GeneratorConfiguration {

  public final GlobalEnvironment environment;

  ExtendedGeneratorConfiguration(GeneratorConfiguration config, GlobalEnvironment environment) {
    super(
        config.interfaceMappingStrategy,
        config.scalarDeserializationStrategy,
        config.typeTransformer,
        config.basePackages,
        config.javaDeprecationConfig);
    this.environment = environment;
  }
}
