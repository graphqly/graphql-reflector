package io.github.graphqly.reflector.execution;

import graphql.relay.Relay;
import io.github.graphqly.reflector.generator.TypeRegistry;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorRegistry;
import io.github.graphqly.reflector.generator.mapping.ConverterRegistry;
import io.github.graphqly.reflector.generator.mapping.InputConverter;
import io.github.graphqly.reflector.generator.mapping.OutputConverter;
import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.metadata.strategy.InclusionStrategy;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;
import io.github.graphqly.reflector.metadata.strategy.type.TypeTransformer;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class GlobalEnvironment {

  public final MessageBundle messageBundle;
  public final Relay relay;
  public final TypeRegistry typeRegistry;
  public final ConverterRegistry converters;
  public final ArgumentInjectorRegistry injectors;
  public final TypeTransformer typeTransformer;
  public final InclusionStrategy inclusionStrategy;
  public final TypeInfoGenerator typeInfoGenerator;

  /**
   * @param messageBundle The global translation message bundle
   * @param relay Relay mapping helper
   * @param typeRegistry The repository of mapped types
   * @param converters Repository of all registered {@link InputConverter}s and {@link
   *     OutputConverter}s
   * @param injectors The repository of registered argument injectors
   * @param typeTransformer Transformer used to pre-process the types (can be used to complete the
   *     missing generics etc)
   * @param inclusionStrategy The strategy that decides which input fields are acceptable
   * @param typeInfoGenerator The generator for type names and descriptions
   */
  public GlobalEnvironment(
      MessageBundle messageBundle,
      Relay relay,
      TypeRegistry typeRegistry,
      ConverterRegistry converters,
      ArgumentInjectorRegistry injectors,
      TypeTransformer typeTransformer,
      InclusionStrategy inclusionStrategy,
      TypeInfoGenerator typeInfoGenerator) {
    this.messageBundle = messageBundle;
    this.relay = relay;
    this.typeRegistry = typeRegistry;
    this.converters = converters;
    this.injectors = injectors;
    this.typeTransformer = typeTransformer;
    this.inclusionStrategy = inclusionStrategy;
    this.typeInfoGenerator = typeInfoGenerator;
  }

  @SuppressWarnings("unchecked")
  public <T, S> T convertInput(S input, AnnotatedType type, ValueMapper valueMapper) {
    if (input == null) {
      return null;
    }
    InputConverter<T, S> inputConverter = this.converters.getInputConverter(type);
    return inputConverter == null
        ? (T) input
        : inputConverter.convertInput(input, type, this, valueMapper);
  }

  public AnnotatedType getMappableInputType(AnnotatedType type) {
    return this.converters.getMappableInputType(type);
  }

  public List<InputConverter> getInputConverters() {
    return this.converters.getInputConverters();
  }
}
