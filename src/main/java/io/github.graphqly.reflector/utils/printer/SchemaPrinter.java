package io.github.graphqly.reflector.utils.printer;

import graphql.Assert;
import graphql.PublicApi;
import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Document;
import graphql.language.Node;
import graphql.language.Value;
import graphql.scalars.object.ObjectScalar;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.DefaultSchemaPrinterComparatorRegistry;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinterComparatorEnvironment;
import graphql.schema.idl.SchemaPrinterComparatorRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import graphql.schema.visibility.GraphqlFieldVisibility;
import io.github.graphqly.reflector.generator.OperationMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * SchemaPrinter A custom version from github.io.github.graphqly.reflector.reflector-java
 * SchemaPrinter
 *
 * <p>This can print an in memory GraphQL schema back to a logical schema definition
 */
@PublicApi
public class SchemaPrinter {

  private final Map<Class, TypePrinter<?>> printers = new LinkedHashMap<>();
  private final Options options;

  public SchemaPrinter() {
    this(Options.defaultOptions());
  }

  public SchemaPrinter(Options options) {
    this.options = options;
    printers.put(GraphQLSchema.class, schemaPrinter());
    printers.put(GraphQLObjectType.class, objectPrinter());
    printers.put(GraphQLEnumType.class, enumPrinter());
    printers.put(GraphQLScalarType.class, scalarPrinter());
    printers.put(GraphQLInterfaceType.class, interfacePrinter());
    printers.put(GraphQLUnionType.class, unionPrinter());
    printers.put(GraphQLInputObjectType.class, inputObjectPrinter());
  }

  private static String printAst(Object value, GraphQLInputType type) {
    Value astValue = AstValueHelperEx.astFromValue(value, type);
    return AstPrinter.printAst(astValue);
  }

  private static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }

  /**
   * This can print an in memory GraphQL IDL document back to a logical schema definition. If you
   * want to turn a Introspection query result into a Document (and then into a printed schema) then
   * use {@link
   * graphql.introspection.IntrospectionResultToSchema#createSchemaDefinition(java.util.Map)} first
   * to get the {@link graphql.language.Document} and then print that.
   *
   * @param schemaIDL the parsed schema IDL
   * @return the logical schema definition
   */
  public String print(Document schemaIDL) {
    TypeDefinitionRegistry registry = new SchemaParser().buildRegistry(schemaIDL);
    return print(UnExecutableSchemaGenerator.makeUnExecutableSchema(registry));
  }

  /**
   * This can print an in memory GraphQL schema back to a logical schema definition
   *
   * @param schema the schema in play
   * @return the logical schema definition
   */
  public String print(GraphQLSchema schema) {
    return print(schema, null);
  }

  public String print(GraphQLSchema schema, OperationMapper operationMapper) {
    StringWriter sw = new StringWriter();
    BlueprintWriter out = BlueprintWriter.getDefault(sw, schema, operationMapper);

    GraphqlFieldVisibility visibility = schema.getCodeRegistry().getFieldVisibility();

    printer(schema.getClass()).print(out, schema, visibility);

    List<GraphQLType> typesAsList =
        schema.getAllTypesAsList().stream()
            .sorted(Comparator.comparing(GraphQLType::getName))
            .collect(toList());

    // Input objects must be resolved first
    // This will ease the process of getting runtime information about object types
    printType(out, typesAsList, GraphQLInputObjectType.class, visibility);

    printType(out, typesAsList, GraphQLInterfaceType.class, visibility);
    printType(out, typesAsList, GraphQLUnionType.class, visibility);
    printType(out, typesAsList, GraphQLObjectType.class, visibility);
    printType(out, typesAsList, GraphQLEnumType.class, visibility);
    printType(out, typesAsList, GraphQLScalarType.class, visibility);

    String result = sw.toString();
    if (result.endsWith("\n\n")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private boolean isIntrospectionType(GraphQLType type) {
    return !options.isIncludeIntrospectionTypes() && type.getName().startsWith("__");
  }

  private TypePrinter<GraphQLScalarType> scalarPrinter() {
    return (out, type, visibility) -> {
      if (!options.isIncludeScalars()) {
        return;
      }
      boolean printScalar;
      if (ScalarInfo.isStandardScalar(type)) {
        printScalar = false;
        //noinspection RedundantIfStatement
        if (options.isIncludeExtendedScalars() && !ScalarInfo.isGraphqlSpecifiedScalar(type)) {
          printScalar = true;
        }
      } else {
        printScalar = true;
      }
      if (printScalar) {
        printComments(out, type, "");
        out.format(
            "scalar %s%s\n\n",
            type.getName(), directivesString(GraphQLScalarType.class, type.getDirectives()));
      }
    };
  }

  private TypePrinter<GraphQLEnumType> enumPrinter() {
    return (out, type, visibility) -> {
      if (isIntrospectionType(type)) {
        return;
      }

      SchemaPrinterComparatorEnvironment environment =
          SchemaPrinterComparatorEnvironment.newEnvironment()
              .parentType(GraphQLEnumType.class)
              .elementType(GraphQLEnumValueDefinition.class)
              .build();
      Comparator<? super GraphQLType> comparator =
          options.comparatorRegistry.getComparator(environment);

      printComments(out, type, "");
      out.format(
          "enum %s%s {\n",
          type.getName(), directivesString(GraphQLEnumType.class, type.getDirectives()));
      List<GraphQLEnumValueDefinition> values =
          type.getValues().stream().sorted(comparator).collect(toList());
      for (GraphQLEnumValueDefinition enumValueDefinition : values) {
        printComments(out, enumValueDefinition, "  ");
        out.format(
            "  %s%s\n",
            enumValueDefinition.getName(),
            directivesString(
                GraphQLEnumValueDefinition.class, enumValueDefinition.getDirectives()));
      }
      out.format("}\n\n");
    };
  }

  private TypePrinter<GraphQLInterfaceType> interfacePrinter() {
    return (out, type, visibility) -> {
      if (isIntrospectionType(type)) {
        return;
      }

      SchemaPrinterComparatorEnvironment environment =
          SchemaPrinterComparatorEnvironment.newEnvironment()
              .parentType(GraphQLInterfaceType.class)
              .elementType(GraphQLFieldDefinition.class)
              .build();
      Comparator<? super GraphQLType> comparator =
          options.comparatorRegistry.getComparator(environment);

      printComments(out, type, "");
      out.format(
          "interface %s%s {\n",
          type.getName(), directivesString(GraphQLInterfaceType.class, type.getDirectives()));
      fieldsPrinter(out, comparator, visibility.getFieldDefinitions(type), type, visibility);
    };
  }

  private void fieldsPrinter(
      BlueprintWriter out,
      Comparator<? super GraphQLType> comparator,
      List<GraphQLFieldDefinition> fieldDefinitions,
      GraphQLType type,
      GraphqlFieldVisibility visibility) {
    fieldDefinitions.stream()
        .sorted(comparator)
        .forEach(
            fd -> {
              printComments(out, fd, "  ");
              GraphQLType fieldType =
                  out.operationMapper.getDynamicFieldType(type, fd, fd.getType());
              out.format(
                  "  %s%s: %s%s\n",
                  fd.getName(),
                  argsString(GraphQLFieldDefinition.class, fd.getArguments()),
                  typeString(fieldType),
                  directivesString(GraphQLFieldDefinition.class, fd.getDirectives()));
            });
    out.format("}\n\n");
  }

  private TypePrinter<GraphQLUnionType> unionPrinter() {
    return (out, type, visibility) -> {
      if (isIntrospectionType(type)) {
        return;
      }

      SchemaPrinterComparatorEnvironment environment =
          SchemaPrinterComparatorEnvironment.newEnvironment()
              .parentType(GraphQLUnionType.class)
              .elementType(GraphQLOutputType.class)
              .build();
      Comparator<? super GraphQLType> comparator =
          options.comparatorRegistry.getComparator(environment);

      printComments(out, type, "");
      out.format(
          "union %s%s = ",
          type.getName(), directivesString(GraphQLUnionType.class, type.getDirectives()));
      List<GraphQLOutputType> types = type.getTypes().stream().sorted(comparator).collect(toList());
      for (int i = 0; i < types.size(); i++) {
        GraphQLOutputType objectType = types.get(i);
        if (i > 0) {
          out.format(" | ");
        }
        out.format("%s", objectType.getName());
      }
      out.format("\n\n");
    };
  }

  private TypePrinter<GraphQLObjectType> objectPrinter() {
    return (out, type, visibility) -> {
      if (isIntrospectionType(type)) {
        return;
      }
      printComments(out, type, "");
      if (type.getInterfaces().isEmpty()) {
        out.format(
            "type %s%s {\n",
            type.getName(), directivesString(GraphQLObjectType.class, type.getDirectives()));
      } else {

        SchemaPrinterComparatorEnvironment environment =
            SchemaPrinterComparatorEnvironment.newEnvironment()
                .parentType(GraphQLObjectType.class)
                .elementType(GraphQLOutputType.class)
                .build();
        Comparator<? super GraphQLType> implementsComparator =
            options.comparatorRegistry.getComparator(environment);

        Stream<String> interfaceNames =
            type.getInterfaces().stream().sorted(implementsComparator).map(GraphQLType::getName);
        out.format(
            "type %s implements %s%s {\n",
            type.getName(),
            interfaceNames.collect(joining(" & ")),
            directivesString(GraphQLObjectType.class, type.getDirectives()));
      }

      SchemaPrinterComparatorEnvironment environment =
          SchemaPrinterComparatorEnvironment.newEnvironment()
              .parentType(GraphQLObjectType.class)
              .elementType(GraphQLFieldDefinition.class)
              .build();
      Comparator<? super GraphQLType> comparator =
          options.comparatorRegistry.getComparator(environment);

      fieldsPrinter(out, comparator, visibility.getFieldDefinitions(type), type, visibility);
    };
  }

  private TypePrinter<GraphQLInputObjectType> inputObjectPrinter() {
    return (out, type, visibility) -> {
      if (isIntrospectionType(type)) {
        return;
      }
      printComments(out, type, "");

      SchemaPrinterComparatorEnvironment environment =
          SchemaPrinterComparatorEnvironment.newEnvironment()
              .parentType(GraphQLInputObjectType.class)
              .elementType(GraphQLInputObjectField.class)
              .build();
      Comparator<? super GraphQLType> comparator =
          options.comparatorRegistry.getComparator(environment);

      out.format(
          "input %s%s {\n",
          type.getName(), directivesString(GraphQLInputObjectType.class, type.getDirectives()));
      visibility.getFieldDefinitions(type).stream()
          .sorted(comparator)
          .forEach(
              fd -> {
                printComments(out, fd, "  ");
                GraphQLInputType fieldType = fd.getType();
                Object defaultValue = fd.getDefaultValue();

                if (defaultValue == null) {
                  out.format("  %s: %s", fd.getName(), typeString(fieldType));
                } else {
                  if (StringUtils.equals(fieldType.getName(), ObjectScalar.class.getSimpleName())) {
                    // updated type with dynamic information resolved during runtime
                    //                    out.updateDynamicFieldType(type, fd, defaultValue);
                    //                    fieldType =
                    // out.operationMapper.toGraphQLInputType(defaultValue);
                    //                    out.updateDynamicFieldType(getInputFieldSignature(type,
                    // fd), fieldType);
                    //                    out.operationMapper.toGraphQLType(defaultValue);
                    fieldType = out.operationMapper.updateDynamicFieldType(type, fd, defaultValue);
                  }

                  out.format("  %s: %s", fd.getName(), typeString(fieldType));

                  String astValue = printAst(defaultValue, fieldType);
                  out.format(" = %s", astValue);
                }
                out.format(directivesString(GraphQLInputObjectField.class, fd.getDirectives()));
                out.format("\n");
              });
      out.format("}\n\n");
    };
  }

  private TypePrinter<GraphQLSchema> schemaPrinter() {
    return (out, type, visibility) -> {
      GraphQLObjectType queryType = type.getQueryType();
      GraphQLObjectType mutationType = type.getMutationType();
      GraphQLObjectType subscriptionType = type.getSubscriptionType();

      // when serializing a GraphQL schema using the type system language, a
      // schema definition should be omitted if only uses the default root type names.
      boolean needsSchemaPrinted = options.includeSchemaDefinition;

      if (!needsSchemaPrinted) {
        if (queryType != null && !queryType.getName().equals("Query")) {
          needsSchemaPrinted = true;
        }
        if (mutationType != null && !mutationType.getName().equals("Mutation")) {
          needsSchemaPrinted = true;
        }
        if (subscriptionType != null && !subscriptionType.getName().equals("Subscription")) {
          needsSchemaPrinted = true;
        }
      }

      if (needsSchemaPrinted) {
        out.format("schema {\n");
        if (queryType != null) {
          out.format("  query: %s\n", queryType.getName());
        }
        if (mutationType != null) {
          out.format("  mutation: %s\n", mutationType.getName());
        }
        if (subscriptionType != null) {
          out.format("  subscription: %s\n", subscriptionType.getName());
        }
        out.format("}\n\n");
      }
    };
  }

  String typeString(GraphQLType rawType) {
    return GraphQLTypeUtil.simplePrint(rawType);
  }

  String argsString(List<GraphQLArgument> arguments) {
    return argsString(null, arguments);
  }

  String argsString(Class<? extends GraphQLType> parent, List<GraphQLArgument> arguments) {
    boolean hasDescriptions =
        arguments.stream().anyMatch(arg -> !isNullOrEmpty(arg.getDescription()));
    String halfPrefix = hasDescriptions ? "  " : "";
    String prefix = hasDescriptions ? "    " : "";
    int count = 0;
    StringBuilder sb = new StringBuilder();

    SchemaPrinterComparatorEnvironment environment =
        SchemaPrinterComparatorEnvironment.newEnvironment()
            .parentType(parent)
            .elementType(GraphQLArgument.class)
            .build();
    Comparator<? super GraphQLType> comparator =
        options.comparatorRegistry.getComparator(environment);

    arguments = arguments.stream().sorted(comparator).collect(toList());
    for (GraphQLArgument argument : arguments) {
      if (count == 0) {
        sb.append("(");
      } else {
        sb.append(", ");
      }
      if (hasDescriptions) {
        sb.append("\n");
      }
      String description = argument.getDescription();
      if (!isNullOrEmpty(description)) {
        String[] descriptionSplitByNewlines = description.split("\n");
        Stream<String> stream = Arrays.stream(descriptionSplitByNewlines);
        if (descriptionSplitByNewlines.length > 1) {
          String multiLineComment = "\"\"\"";
          stream = Stream.concat(Stream.of(multiLineComment), stream);
          stream = Stream.concat(stream, Stream.of(multiLineComment));
          stream.map(s -> prefix + s + "\n").forEach(sb::append);
        } else {
          stream.map(s -> prefix + "#" + s + "\n").forEach(sb::append);
        }
      }
      sb.append(prefix)
          .append(argument.getName())
          .append(": ")
          .append(typeString(argument.getType()));
      Object defaultValue = argument.getDefaultValue();
      if (defaultValue != null) {
        sb.append(" = ");
        sb.append(printAst(defaultValue, argument.getType()));
      }

      argument.getDirectives().stream()
          .map(this::directiveString)
          .filter(it -> !it.isEmpty())
          .forEach(directiveString -> sb.append(" ").append(directiveString));

      count++;
    }
    if (count > 0) {
      if (hasDescriptions) {
        sb.append("\n");
      }
      sb.append(halfPrefix).append(")");
    }
    return sb.toString();
  }

  String directivesString(Class<? extends GraphQLType> parent, List<GraphQLDirective> directives) {
    if (!options.includeDirectives) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    if (!directives.isEmpty()) {
      sb.append(" ");
    }

    SchemaPrinterComparatorEnvironment environment =
        SchemaPrinterComparatorEnvironment.newEnvironment()
            .parentType(parent)
            .elementType(GraphQLDirective.class)
            .build();
    Comparator<? super GraphQLType> comparator =
        options.comparatorRegistry.getComparator(environment);

    directives =
        directives.stream()
            .sorted(comparator)
            .filter(
                directive -> {
                  String name = directive.getName();
                  for (String ignored : options.ignoredDirectives) {
                    if (name.matches(ignored)) {
                      return false;
                    }
                  }
                  return true;
                })
            .collect(toList());
    for (int i = 0; i < directives.size(); i++) {
      GraphQLDirective directive = directives.get(i);
      sb.append(directiveString(directive));
      if (i < directives.size() - 1) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  private String directiveString(GraphQLDirective directive) {
    if (!options.includeDirectives) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("@").append(directive.getName());

    SchemaPrinterComparatorEnvironment environment =
        SchemaPrinterComparatorEnvironment.newEnvironment()
            .parentType(GraphQLDirective.class)
            .elementType(GraphQLArgument.class)
            .build();
    Comparator<? super GraphQLType> comparator =
        options.comparatorRegistry.getComparator(environment);

    List<GraphQLArgument> args = directive.getArguments();
    args = args.stream().sorted(comparator).collect(toList());
    if (!args.isEmpty()) {
      sb.append("(");
      for (int i = 0; i < args.size(); i++) {
        GraphQLArgument arg = args.get(i);
        sb.append(arg.getName());
        if (arg.getValue() != null) {
          sb.append(" : ");
          sb.append(printAst(arg.getValue(), arg.getType()));
        } else if (arg.getDefaultValue() != null) {
          sb.append(" : ");
          sb.append(printAst(arg.getDefaultValue(), arg.getType()));
        }
        if (i < args.size() - 1) {
          sb.append(", ");
        }
      }
      sb.append(")");
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private <T> TypePrinter<T> printer(Class<?> clazz) {
    TypePrinter typePrinter =
        printers.computeIfAbsent(
            clazz,
            k -> {
              Class<?> superClazz = clazz.getSuperclass();
              TypePrinter result;
              if (superClazz != Object.class) {
                result = printer(superClazz);
              } else {
                result = (out, type, visibility) -> out.println("Type not implemented : " + type);
              }
              return result;
            });
    return (TypePrinter<T>) typePrinter;
  }

  @SuppressWarnings("unchecked")
  private void printType(
      BlueprintWriter out,
      List<GraphQLType> typesAsList,
      Class typeClazz,
      GraphqlFieldVisibility visibility) {
    typesAsList.stream()
        .filter(type -> typeClazz.isAssignableFrom(type.getClass()))
        .forEach(type -> printType(out, type, visibility));
  }

  private void printType(BlueprintWriter out, GraphQLType type, GraphqlFieldVisibility visibility) {
    TypePrinter<Object> printer = printer(type.getClass());
    printer.print(out, type, visibility);
  }

  //  public String print(GraphQLType type) {
  //    StringWriter sw = new StringWriter();
  //    PrintWriter out = new PrintWriter(sw);
  //
  //    printType(out, type, DEFAULT_FIELD_VISIBILITY);
  //
  //    return sw.toString();
  //  }

  private void printComments(BlueprintWriter out, Object graphQLType, String prefix) {

    AstDescriptionAndComments descriptionAndComments = getDescriptionAndComments(graphQLType);
    if (descriptionAndComments == null) {
      return;
    }

    Description astDescription = descriptionAndComments.descriptionAst;
    if (astDescription != null) {
      String quoteStr = "\"";
      if (astDescription.isMultiLine()) {
        quoteStr = "\"\"\"";
      }
      out.write(prefix);
      out.write(quoteStr);
      out.write(astDescription.getContent());
      out.write(quoteStr);
      out.write("\n");

      return;
    }

    String commentStart = prefix + "# ";

    if (descriptionAndComments.comments != null) {
      descriptionAndComments.comments.forEach(
          cmt -> {
            out.write(commentStart);
            out.write(cmt.getContent());
            out.write("\n");
          });
    } else {
      String runtimeDescription = descriptionAndComments.runtimeDescription;
      if (!isNullOrEmpty(runtimeDescription)) {
        Stream<String> stream = Arrays.stream(runtimeDescription.split("\n"));
        stream.map(s -> commentStart + s + "\n").forEach(out::write);
      }
    }
  }

  private AstDescriptionAndComments getDescriptionAndComments(Object descriptionHolder) {
    if (descriptionHolder instanceof GraphQLObjectType) {
      GraphQLObjectType type = (GraphQLObjectType) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLEnumType) {
      GraphQLEnumType type = (GraphQLEnumType) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLFieldDefinition) {
      GraphQLFieldDefinition type = (GraphQLFieldDefinition) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLEnumValueDefinition) {
      GraphQLEnumValueDefinition type = (GraphQLEnumValueDefinition) descriptionHolder;
      return descriptionAndComments(type::getDescription, () -> null, () -> null);
    } else if (descriptionHolder instanceof GraphQLUnionType) {
      GraphQLUnionType type = (GraphQLUnionType) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLInputObjectType) {
      GraphQLInputObjectType type = (GraphQLInputObjectType) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLInputObjectField) {
      GraphQLInputObjectField type = (GraphQLInputObjectField) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLInterfaceType) {
      GraphQLInterfaceType type = (GraphQLInterfaceType) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLScalarType) {
      GraphQLScalarType type = (GraphQLScalarType) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else if (descriptionHolder instanceof GraphQLArgument) {
      GraphQLArgument type = (GraphQLArgument) descriptionHolder;
      return descriptionAndComments(
          type::getDescription, type::getDefinition, () -> type.getDefinition().getDescription());
    } else {
      return Assert.assertShouldNeverHappen();
    }
  }

  AstDescriptionAndComments descriptionAndComments(
      Supplier<String> stringSupplier,
      Supplier<Node> nodeSupplier,
      Supplier<Description> descriptionSupplier) {
    String runtimeDesc = stringSupplier.get();
    Node node = nodeSupplier.get();
    Description description = null;
    List<Comment> comments = null;
    if (node != null) {
      comments = node.getComments();
      description = descriptionSupplier.get();
    }
    return new AstDescriptionAndComments(runtimeDesc, description, comments);
  }

  private interface TypePrinter<T> {

    void print(BlueprintWriter out, T type, GraphqlFieldVisibility visibility);
  }

  /** Options to use when printing a schema */
  public static class Options {

    private boolean includeIntrospectionTypes;

    private boolean includeScalars;

    private boolean includeExtendedScalars;

    private boolean includeSchemaDefinition;

    private boolean includeDirectives;

    private final Set<String> ignoredDirectives = new HashSet<>();

    private SchemaPrinterComparatorRegistry comparatorRegistry;

    private Options(
        boolean includeIntrospectionTypes,
        boolean includeScalars,
        boolean includeExtendedScalars,
        boolean includeSchemaDefinition,
        boolean includeDirectives,
        SchemaPrinterComparatorRegistry comparatorRegistry) {
      this.includeIntrospectionTypes = includeIntrospectionTypes;
      this.includeScalars = includeScalars;
      this.includeExtendedScalars = includeExtendedScalars;
      this.includeSchemaDefinition = includeSchemaDefinition;
      this.includeDirectives = includeDirectives;
      this.comparatorRegistry = comparatorRegistry;
    }

    public static Options defaultOptions() {
      return new Options(
          false,
          false,
          false,
          false,
          true,
          DefaultSchemaPrinterComparatorRegistry.defaultComparators());
    }

    public boolean isIncludeIntrospectionTypes() {
      return includeIntrospectionTypes;
    }

    public boolean isIncludeScalars() {
      return includeScalars;
    }

    public boolean isIncludeExtendedScalars() {
      return includeExtendedScalars;
    }

    public boolean isIncludeSchemaDefinition() {
      return includeSchemaDefinition;
    }

    public boolean isIncludeDirectives() {
      return includeDirectives;
    }

    /**
     * This will allow you to include introspection types that are contained in a schema
     *
     * @param flag whether to include them
     * @return options
     */
    public Options includeIntrospectionTypes(boolean flag) {
      this.includeIntrospectionTypes = flag;
      return this;
    }

    /**
     * This will allow you to include scalar types that are contained in a schema
     *
     * @param flag whether to include them
     * @return options
     */
    public Options includeScalarTypes(boolean flag) {
      this.includeScalars = flag;
      return this;
    }

    /**
     * This will allow you to include the github.io.github.graphqly.reflector.reflector 'extended'
     * scalar types that come with github.io.github.graphqly.reflector.reflector-java such as
     * GraphQLBigDecimal or GraphQLBigInteger
     *
     * @param flag whether to include them
     * @return options
     */
    public Options includeExtendedScalarTypes(boolean flag) {
      this.includeExtendedScalars = flag;
      return this;
    }

    /**
     * This will force the printing of the github.io.github.graphqly.reflector.reflector schema
     * definition even if the query, mutation, and/or subscription types use the default names. Some
     * github.io.github.graphqly.reflector.reflector parsers require this information even if the
     * schema uses the default type names. The schema definition will always be printed if any of
     * the query, mutation, or subscription types do not use the default names.
     *
     * @param flag whether to force include the schema definition
     * @return options
     */
    public Options includeSchemaDefintion(boolean flag) {
      this.includeSchemaDefinition = flag;
      return this;
    }

    /**
     * Allow to print directives. In some situations, auto-generated schemas contain a lot of
     * directives that make the printout noisy and having this flag would allow cleaner printout. On
     * by default.
     *
     * @param flag whether to print directives
     * @return new instance of options
     */
    public Options includeDirectives(boolean flag) {
      this.ignoredDirectives.clear();
      this.includeDirectives = flag;
      return this;
    }

    public Options includeDirectives() {
      return this.includeDirectives(true);
    }

    public Options excludeDirectives(String... ignoredPatterns) {
      this.ignoredDirectives.clear();
      this.ignoredDirectives.addAll(Arrays.asList(ignoredPatterns));
      this.includeDirectives = true;
      return this;
    }

    /**
     * The comparator registry controls the printing order for registered {@code GraphQLType}s.
     *
     * @param comparatorRegistry The registry containing the {@code Comparator} and environment
     *     scoping rules.
     * @return options
     */
    public Options setComparators(SchemaPrinterComparatorRegistry comparatorRegistry) {
      this.comparatorRegistry = comparatorRegistry;
      return this;
    }

    public SchemaPrinterComparatorRegistry getComparatorRegistry() {
      return comparatorRegistry;
    }
  }

  static class AstDescriptionAndComments {

    String runtimeDescription;

    Description descriptionAst;

    List<Comment> comments;

    public AstDescriptionAndComments(
        String runtimeDescription, Description descriptionAst, List<Comment> comments) {
      this.runtimeDescription = runtimeDescription;
      this.descriptionAst = descriptionAst;
      this.comments = comments;
    }
  }
}
