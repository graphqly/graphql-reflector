package io.github.graphqly.reflector.execution;

import graphql.execution.ExecutionStepInfo;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.github.graphqly.reflector.generator.mapping.ArgumentInjectorParams;
import io.github.graphqly.reflector.generator.mapping.ConverterRegistry;
import io.github.graphqly.reflector.generator.mapping.DelegatingOutputConverter;
import io.github.graphqly.reflector.generator.mapping.OutputConverter;
import io.github.graphqly.reflector.metadata.OperationArgument;
import io.github.graphqly.reflector.metadata.Resolver;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.github.graphqly.reflector.util.Urls;

import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author Bojan Tomic (kaqqao) */
@SuppressWarnings("WeakerAccess")
public class ResolutionEnvironment {

  public final Object context;
  public final Object rootContext;
  public final Resolver resolver;
  public final ValueMapper valueMapper;
  public final GlobalEnvironment globalEnvironment;
  public final List<Field> fields;
  public final GraphQLOutputType fieldType;
  public final GraphQLType parentType;
  public final GraphQLSchema graphQLSchema;
  public final DataFetchingEnvironment dataFetchingEnvironment;
  public final Map<String, Object> arguments;

  private final ConverterRegistry converters;
  private final DerivedTypeRegistry derivedTypes;

  public ResolutionEnvironment(
      Resolver resolver,
      DataFetchingEnvironment env,
      ValueMapper valueMapper,
      GlobalEnvironment globalEnvironment,
      ConverterRegistry converters,
      DerivedTypeRegistry derivedTypes) {

    this.context = env.getSource();
    this.rootContext = env.getContext();
    this.resolver = resolver;
    this.valueMapper = valueMapper;
    this.globalEnvironment = globalEnvironment;
    this.converters = converters;
    this.fields = env.getFields();
    this.fieldType = env.getFieldType();
    this.parentType = env.getParentType();
    this.graphQLSchema = env.getGraphQLSchema();
    this.dataFetchingEnvironment = env;
    this.derivedTypes = derivedTypes;
    this.arguments = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  public <T, S> S convertOutput(T output, AnnotatedType type) {
    if (output == null) {
      return null;
    }
    OutputConverter<T, S> outputConverter = converters.getOutputConverter(type);
    return outputConverter == null ? (S) output : outputConverter.convertOutput(output, type, this);
  }

  public AnnotatedType getDerived(AnnotatedType type, int index) {
    try {
      return getDerived(type).get(index);
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException(
          String.format(
              "No type derived from %s found at index %d. "
                  + "Make sure the converter implements %s and provides the derived types correctly. "
                  + "See %s for details and possible solutions.",
              type.getType().getTypeName(),
              index,
              DelegatingOutputConverter.class.getSimpleName(),
              Urls.Errors.DERIVED_TYPES),
          e);
    }
  }

  public List<AnnotatedType> getDerived(AnnotatedType type) {
    return derivedTypes.getDerived(type);
  }

  public Object getInputValue(Object input, OperationArgument argument) {
    boolean argValuePresent = dataFetchingEnvironment.containsArgument(argument.getName());
    ArgumentInjectorParams params =
        new ArgumentInjectorParams(
            input,
            argValuePresent,
            argument.getJavaType(),
            argument.getBaseType(),
            argument.getParameter(),
            this);
    Object value =
        this.globalEnvironment
            .injectors
            .getInjector(argument.getJavaType(), argument.getParameter())
            .getArgumentValue(params);
    if (argValuePresent) {
      arguments.put(argument.getName(), value);
    }
    return value;
  }

  public Directives getDirectives(ExecutionStepInfo step) {
    return new Directives(dataFetchingEnvironment, step);
  }

  public Directives getDirectives() {
    return getDirectives(null);
  }
}
