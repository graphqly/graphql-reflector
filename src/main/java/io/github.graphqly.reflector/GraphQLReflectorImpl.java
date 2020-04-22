package io.github.graphqly.reflector;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;
import io.github.graphqly.reflector.generator.mapping.common.ScalarMapper;
import io.github.graphqly.reflector.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.github.graphqly.reflector.metadata.strategy.value.manifest.ManifestValueMapperFactory;
import io.github.graphqly.reflector.module.common.gson.GsonScalarTypeMapper;
import io.github.graphqly.reflector.utils.printer.FunctionPrinter;
import io.github.graphqly.reflector.utils.printer.SchemaPrinter;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class GraphQLReflectorImpl extends GraphQLReflector {

  private GraphQLSchema schema;
  private GraphQLSchemaGenerator schemaGenerator;

  protected GraphQLReflectorImpl(AnnotatedType rootType) {
    super(rootType);
  }

  protected GraphQLReflectorImpl(AnnotatedType rootType, Map<AnnotatedType, Object> defaultValues) {
    super(rootType, defaultValues);
  }

  public static List<AnnotatedType> resolveAdditionalJavaTypes(
      AnnotatedType prototype, Object instance) {
    Field[] fields = ((Class) (prototype.getType())).getFields();
    return Arrays.stream(fields)
        .map(
            field -> {
              try {
                Object value = field.get(instance);

                if (value == null || value instanceof List) {
                  return null;
                }
                Class<?> actualType = value.getClass();
                Class<?> declaredType = field.getType();

                if (actualType.equals(declaredType)) {
                  return null;
                }

                // drop several types already registered
                if (actualType.getSimpleName().equals("JsonObject")) {
                  return null;
                }

                return GenericTypeReflector.annotate(actualType);
              } catch (IllegalAccessException e) {
                return null;
              }
            })
        .filter(type -> type != null)
        .collect(Collectors.toList());
  }

  private static List<AnnotatedType> resolveAdditionalJavaTypes(
      Map<AnnotatedType, Object> defaultValues) {
    List<AnnotatedType> result = new ArrayList<>();
    defaultValues.forEach(
        (prototype, value) -> {
          result.addAll(resolveAdditionalJavaTypes(prototype, value));
        });
    return result;
  }

  @Override
  public void build(AnnotatedType rootType, Map<AnnotatedType, Object> defaultValues) {
    RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime);

    List<AnnotatedType> additionalJavaTypes = resolveAdditionalJavaTypes(defaultValues);

    TypeMapper[] typeMappers = {new GsonScalarTypeMapper(), new ScalarMapper()};
    ManifestValueMapperFactory valueMapperFactory = new ManifestValueMapperFactory();
    defaultValues.forEach(
        (prototype, value) -> {
          valueMapperFactory.map(prototype, value);
        });

    schemaGenerator = new GraphQLSchemaGenerator();
    schema =
        schemaGenerator
            .withValueMapperFactory(valueMapperFactory)
            .withTypeMappers(typeMappers)
            .withTypeInfoGenerator(new DefaultTypeInfoGenerator())
            .withAdditionalJavaTypes(additionalJavaTypes)
            .withOperationsFromTypes(rootType)
            .generate();
  }

  @Override
  public String inspectOperation(String name) {
    GraphQLObjectType mutationType = schema.getMutationType();
    boolean isMutationFunction =
            mutationType != null && mutationType.getFieldDefinition(name) != null;

    try {
      FunctionPrinter functionPrinter =
          FunctionPrinter.builder()
              .operationMapper(schemaGenerator.getLastOperationMapper())
              .schema(schema)
              .build();
      return isMutationFunction
          ? functionPrinter.printMutation(name)
          : functionPrinter.printQuery(name);

    } catch (Exception e) {
      e.printStackTrace();
      return "<ERR>";
    }
  }

  @Override
  public String inspectSchema() {
    SchemaPrinter.Options opt =
        SchemaPrinter.Options.defaultOptions()
            .includeDirectives(true)
            // currently we don't want print out scalars
            .includeScalarTypes(false)
            .excludeDirectives("_.*");
    SchemaPrinter printer = new SchemaPrinter(opt);
    return printer.print(schema, schemaGenerator.getLastOperationMapper());
  }
}
