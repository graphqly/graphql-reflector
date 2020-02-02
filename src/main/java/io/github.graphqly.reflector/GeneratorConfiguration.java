package io.github.graphqly.reflector;

import io.github.graphqly.reflector.metadata.strategy.type.TypeTransformer;
import io.github.graphqly.reflector.metadata.strategy.value.ScalarDeserializationStrategy;
import io.github.graphqly.reflector.generator.JavaDeprecationMappingConfig;
import io.github.graphqly.reflector.generator.mapping.strategy.InterfaceMappingStrategy;

@SuppressWarnings("WeakerAccess")
public class GeneratorConfiguration {
  public final InterfaceMappingStrategy interfaceMappingStrategy;
  public final ScalarDeserializationStrategy scalarDeserializationStrategy;
  public final TypeTransformer typeTransformer;
  public final String[] basePackages;
  public final JavaDeprecationMappingConfig javaDeprecationConfig;

  GeneratorConfiguration(
      InterfaceMappingStrategy interfaceMappingStrategy,
      ScalarDeserializationStrategy scalarDeserializationStrategy,
      TypeTransformer typeTransformer,
      String[] basePackages,
      JavaDeprecationMappingConfig javaDeprecationConfig) {
    this.interfaceMappingStrategy = interfaceMappingStrategy;
    this.scalarDeserializationStrategy = scalarDeserializationStrategy;
    this.typeTransformer = typeTransformer;
    this.basePackages = basePackages;
    this.javaDeprecationConfig = javaDeprecationConfig;
  }
}
