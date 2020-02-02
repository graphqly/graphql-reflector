package io.github.graphqly.reflector.generator;

import graphql.GraphQLContext;
import graphql.execution.batched.BatchedDataFetcher;
import graphql.relay.Relay;
import graphql.schema.*;
import io.github.graphqly.reflector.annotations.GraphQLId;
import io.github.graphqly.reflector.execution.ContextWrapper;
import io.github.graphqly.reflector.execution.OperationExecutor;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;
import io.github.graphqly.reflector.metadata.*;
import io.github.graphqly.reflector.metadata.exceptions.MappingException;
import io.github.graphqly.reflector.metadata.strategy.query.DirectiveBuilderParams;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.github.graphqly.reflector.util.Directives;
import io.github.graphqly.reflector.util.GraphQLUtils;
import io.github.graphqly.reflector.util.Urls;
import io.leangen.geantyref.GenericTypeReflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static io.github.graphqly.reflector.util.GraphQLUtils.CLIENT_MUTATION_ID;

/**
 * Drives the work of mapping Java structures into their GraphQL representations. While the task of
 * mapping types is delegated to instances of {@link TypeMapper}, selection of mappers, construction
 * and attachment of resolvers (modeled by {@link DataFetcher}s in <a
 * href=https://github.com/graphql-java/graphql-java>github.io.github.graphqly.reflector.reflector-java</a>), and other universal
 * tasks are encapsulated in this class.
 */
public class OperationMapper {

  private static final Logger log = LoggerFactory.getLogger(OperationMapper.class);
  public final BuildContext buildContext;
  public final Map<String, GraphQLType> dynamicFieldTypes = new HashMap<>();
  private List<GraphQLFieldDefinition> queries; // The list of all mapped queries
  private List<GraphQLFieldDefinition> mutations; // The list of all mapped mutations
  private List<GraphQLFieldDefinition> subscriptions; // The list of all mapped subscriptions
  private List<GraphQLDirective> directives; // The list of all added mapped directives

  /**
   * @param buildContext The shared context containing all the global information needed for mapping
   */
  public OperationMapper(
      String queryRoot, String mutationRoot, String subscriptionRoot, BuildContext buildContext) {
    this.buildContext = buildContext;
    this.queries = generateQueries(queryRoot, buildContext);
    this.mutations = generateMutations(mutationRoot, buildContext);
    this.subscriptions = generateSubscriptions(subscriptionRoot, buildContext);
    this.directives = generateDirectives(buildContext);
    buildContext.resolveTypeReferences();
  }

  /**
   * Generates {@link GraphQLFieldDefinition}s representing all top-level queries (with their types,
   * arguments and sub-queries) fully mapped. This is the entry point into the query-mapping logic.
   *
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return A list of {@link GraphQLFieldDefinition}s representing all top-level queries
   */
  private List<GraphQLFieldDefinition> generateQueries(
      String queryRoot, BuildContext buildContext) {
    List<Operation> rootQueries = new ArrayList<>(buildContext.operationRegistry.getRootQueries());
    List<GraphQLFieldDefinition> queries =
        rootQueries.stream()
            .map(query -> toGraphQLField(queryRoot, query, buildContext))
            .collect(Collectors.toList());

    buildContext.resolveTypeReferences();
    // Add support for the Relay Node query only if an explicit one isn't already provided and
    // Relay-enabled resolvers exist
    if (rootQueries.stream().noneMatch(query -> query.getName().equals(GraphQLUtils.NODE))) {
      Map<String, String> nodeQueriesByType =
          getNodeQueriesByType(
              rootQueries, queries, buildContext.typeRegistry, buildContext.node, buildContext);
      if (!nodeQueriesByType.isEmpty()) {
        queries.add(
            buildContext.relay.nodeField(
                buildContext.node, createNodeResolver(nodeQueriesByType, buildContext.relay)));
      }
    }
    return queries;
  }

  /**
   * Generates {@link GraphQLFieldDefinition}s representing all mutations (with their types,
   * arguments and sub-queries) fully mapped. By the GraphQL spec, all mutations are top-level only.
   * This is the entry point into the mutation-mapping logic.
   *
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return A list of {@link GraphQLFieldDefinition}s representing all mutations
   */
  private List<GraphQLFieldDefinition> generateMutations(
      String mutationRoot, BuildContext buildContext) {
    return buildContext.operationRegistry.getMutations().stream()
        .map(
            mutation ->
                buildContext.relayMappingConfig.relayCompliantMutations
                    ? toRelayMutation(
                        mutationRoot,
                        toGraphQLField(mutation, buildContext),
                        createResolver(mutation, buildContext),
                        buildContext)
                    : toGraphQLField(mutationRoot, mutation, buildContext))
        .collect(Collectors.toList());
  }

  private List<GraphQLFieldDefinition> generateSubscriptions(
      String subscriptionRoot, BuildContext buildContext) {
    return buildContext.operationRegistry.getSubscriptions().stream()
        .map(subscription -> toGraphQLField(subscriptionRoot, subscription, buildContext))
        .collect(Collectors.toList());
  }

  private List<GraphQLDirective> generateDirectives(BuildContext buildContext) {
    return buildContext.additionalDirectives.stream()
        .map(
            directiveType ->
                buildContext.directiveBuilder.buildClientDirective(
                    directiveType, buildContext.directiveBuilderParams()))
        .map(directive -> toGraphQLDirective(directive, buildContext))
        .collect(Collectors.toList());
  }

  private GraphQLFieldDefinition toGraphQLField(Operation operation, BuildContext buildContext) {
    GraphQLOutputType type = toGraphQLType(operation.getJavaType(), buildContext);
    GraphQLFieldDefinition.Builder fieldBuilder =
        newFieldDefinition()
            .name(operation.getName())
            .description(operation.getDescription())
            .deprecate(operation.getDeprecationReason())
            .type(type)
            .withDirective(Directives.mappedOperation(operation))
            .withDirectives(
                toGraphQLDirectives(
                    operation.getTypedElement(),
                    buildContext.directiveBuilder::buildFieldDefinitionDirectives,
                    buildContext));

    List<GraphQLArgument> arguments =
        operation.getArguments().stream()
            .filter(OperationArgument::isMappable)
            .map(argument -> toGraphQLArgument(argument, buildContext))
            .collect(Collectors.toList());
    fieldBuilder.arguments(arguments);
    if (GraphQLUtils.isRelayConnectionType(type)
        && buildContext.relayMappingConfig.strictConnectionSpec) {
      validateConnectionSpecCompliance(operation.getName(), arguments, buildContext.relay);
    }

    return buildContext.transformers.transform(fieldBuilder.build(), operation, this, buildContext);
  }

  /**
   * Maps a single operation to a GraphQL output field (as queries in GraphQL are nothing but fields
   * of the root operation type).
   *
   * @param operation The operation to map to a GraphQL output field
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return GraphQL output field representing the given operation
   */
  public GraphQLFieldDefinition toGraphQLField(
      String parentType, Operation operation, BuildContext buildContext) {
    GraphQLFieldDefinition field = toGraphQLField(operation, buildContext);
    DataFetcher<?> resolver = createResolver(operation, buildContext);
    buildContext.codeRegistry.dataFetcher(coordinates(parentType, field.getName()), resolver);
    return field;
  }

  public GraphQLInputType toGraphQLInputType(Object object) {
    AnnotatedType annotatedType = GenericTypeReflector.annotate(object.getClass());
    return toGraphQLInputType(annotatedType);
  }

  public GraphQLInputType toGraphQLInputType(AnnotatedType annotatedType) {
    GraphQLInputType inputType = toGraphQLInputType(annotatedType, new HashSet<>(), buildContext);
    return inputType;
  }

  public GraphQLOutputType toGraphQLType(Object object) {
    AnnotatedType annotatedType = GenericTypeReflector.annotate(object.getClass());
    HashSet<Class<? extends TypeMapper>> mappersToSkip = new HashSet<>();
    return toGraphQLType(annotatedType, mappersToSkip, buildContext);
  }

  public GraphQLOutputType toGraphQLType(AnnotatedType javaType) {
    return toGraphQLType(javaType, buildContext);
  }

  public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext) {
    return toGraphQLType(javaType, new HashSet<>(), buildContext);
  }

  /**
   * Maps a Java type to a GraphQL output type. Delegates most of the work to applicable {@link
   * TypeMapper}s.
   *
   * <p>See {@link TypeMapper#toGraphQLType(AnnotatedType, OperationMapper, java.util.Set,
   * BuildContext)}
   *
   * @param javaType The Java type that is to be mapped to a GraphQL output type
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return GraphQL output type corresponding to the given Java type
   */
  public GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {

    Annotation[] annotations = javaType.getAnnotations();
    GraphQLOutputType type =
        buildContext
            .typeMappers
            .getTypeMapper(javaType, mappersToSkip)
            .toGraphQLType(javaType, this, mappersToSkip, buildContext);
    if (type instanceof GraphQLTypeReference) {
      return (GraphQLOutputType) buildContext.typeCache.resolveType(type.getName());
    }
    log(buildContext.validator.checkUniqueness(type, javaType));
    buildContext.typeCache.completeType(type);
    return type;
  }

  private AnnotatedType dynamicResolveType(AnnotatedType javaType) {
    //      if(javaType instanceof  AnnotatedTypeFactory.AnnotatedTypeBaseImpl)
    return null;
  }

  /**
   * Maps a single field/property to a GraphQL input field.
   *
   * @param inputField The field/property to map to a GraphQL input field
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return GraphQL input field representing the given field/property
   */
  public GraphQLInputObjectField toGraphQLInputField(
      InputField inputField, BuildContext buildContext) {
    GraphQLInputObjectField.Builder builder =
        newInputObjectField()
            .name(inputField.getName())
            .description(inputField.getDescription())
            .type(toGraphQLInputType(inputField.getJavaType(), buildContext))
            .withDirective(Directives.mappedInputField(inputField))
            .withDirectives(
                toGraphQLDirectives(
                    inputField.getTypedElement(),
                    buildContext.directiveBuilder::buildInputFieldDefinitionDirectives,
                    buildContext))
            .defaultValue(inputField.getDefaultValue());
    return buildContext.transformers.transform(builder.build(), inputField, this, buildContext);
  }

  public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext) {
    return toGraphQLInputType(javaType, new HashSet<>(), buildContext);
  }

  /**
   * Maps a Java type to a GraphQL input type. Delegates most of the work to applicable {@link
   * TypeMapper}s.
   *
   * <p>See {@link TypeMapper#toGraphQLInputType(AnnotatedType, OperationMapper, java.util.Set,
   * BuildContext)}
   *
   * @param javaType The Java type that is to be mapped to a GraphQL input type
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return GraphQL input type corresponding to the given Java type
   */
  public GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    GraphQLInputType type =
        buildContext
            .typeMappers
            .getTypeMapper(javaType, mappersToSkip)
            .toGraphQLInputType(javaType, this, mappersToSkip, buildContext);

    if (type instanceof GraphQLTypeReference) {
      GraphQLInputType resolvableType =
          (GraphQLInputType) buildContext.typeCache.resolveType(type.getName());
      if (resolvableType != null) type = resolvableType;
    }
    log(buildContext.validator.checkUniqueness(type, javaType));
    return type;
  }

  private GraphQLArgument toGraphQLArgument(
      OperationArgument operationArgument, BuildContext buildContext) {
    GraphQLArgument argument =
        newArgument()
            .name(operationArgument.getName())
            .description(operationArgument.getDescription())
            .type(toGraphQLInputType(operationArgument.getJavaType(), buildContext))
            .defaultValue(operationArgument.getDefaultValue())
            .withDirectives(
                toGraphQLDirectives(
                    operationArgument.getTypedElement(),
                    buildContext.directiveBuilder::buildArgumentDefinitionDirectives,
                    buildContext))
            .build();
    return buildContext.transformers.transform(argument, operationArgument, this, buildContext);
  }

  private GraphQLDirective[] toGraphQLDirectives(
      TypedElement element,
      BiFunction<AnnotatedElement, DirectiveBuilderParams, List<Directive>> directiveBuilder,
      BuildContext buildContext) {
    return element.getElements().stream()
        .flatMap(el -> directiveBuilder.apply(el, buildContext.directiveBuilderParams()).stream())
        .map(directive -> toGraphQLDirective(directive, buildContext))
        .toArray(GraphQLDirective[]::new);
  }

  public GraphQLDirective toGraphQLDirective(Directive directive, BuildContext buildContext) {
    GraphQLDirective.Builder builder =
        GraphQLDirective.newDirective()
            .name(directive.getName())
            .description(directive.getDescription())
            .validLocations(directive.getLocations());
    directive.getArguments().forEach(arg -> builder.argument(toGraphQLArgument(arg, buildContext)));
    return buildContext.transformers.transform(builder.build(), directive, this, buildContext);
  }

  private GraphQLArgument toGraphQLArgument(
      DirectiveArgument directiveArgument, BuildContext buildContext) {
    GraphQLArgument argument =
        newArgument()
            .name(directiveArgument.getName())
            .description(directiveArgument.getDescription())
            .type(toGraphQLInputType(directiveArgument.getJavaType(), buildContext))
            .value(directiveArgument.getValue())
            .defaultValue(directiveArgument.getDefaultValue())
            .withDirectives(
                toGraphQLDirectives(
                    directiveArgument.getTypedElement(),
                    buildContext.directiveBuilder::buildArgumentDefinitionDirectives,
                    buildContext))
            .build();
    return buildContext.transformers.transform(argument, directiveArgument, this, buildContext);
  }

  private GraphQLFieldDefinition toRelayMutation(
      String parentType,
      GraphQLFieldDefinition mutation,
      DataFetcher<?> resolver,
      BuildContext buildContext) {

    String payloadTypeName = mutation.getName() + "Payload";
    List<GraphQLFieldDefinition> outputFields;
    if (mutation.getType() instanceof GraphQLObjectType) {
      outputFields = ((GraphQLObjectType) mutation.getType()).getFieldDefinitions();
    } else {
      outputFields = new ArrayList<>();
      outputFields.add(
          GraphQLFieldDefinition.newFieldDefinition()
              .name(buildContext.relayMappingConfig.wrapperFieldName)
              .description(buildContext.relayMappingConfig.wrapperFieldDescription)
              .type(mutation.getType())
              .build());
      DataFetcher<?> wrapperFieldResolver = DataFetchingEnvironment::getSource;
      buildContext.codeRegistry.dataFetcher(
          coordinates(payloadTypeName, buildContext.relayMappingConfig.wrapperFieldName),
          wrapperFieldResolver);
    }
    List<GraphQLInputObjectField> inputFields =
        mutation.getArguments().stream()
            .map(
                arg ->
                    newInputObjectField()
                        .name(arg.getName())
                        .description(arg.getDescription())
                        .type(arg.getType())
                        .defaultValue(arg.getDefaultValue())
                        .build())
            .collect(Collectors.toList());

    GraphQLInputObjectType inputObjectType =
        newInputObject()
            .name(mutation.getName() + TypeInfoGenerator.INPUT_SUFFIX)
            .field(
                newInputObjectField()
                    .name(CLIENT_MUTATION_ID)
                    .type(new GraphQLNonNull(GraphQLString)))
            .fields(inputFields)
            .build();

    GraphQLObjectType outputType =
        newObject()
            .name(payloadTypeName)
            .field(
                newFieldDefinition()
                    .name(CLIENT_MUTATION_ID)
                    .type(new GraphQLNonNull(GraphQLString)))
            .fields(outputFields)
            .build();

    DataFetcher<String> simpleResolver = new PropertyDataFetcher<>(CLIENT_MUTATION_ID);
    DataFetcher<String> clientMutationIdResolver =
        env -> {
          if (env.getContext() instanceof ContextWrapper) {
            return ((ContextWrapper) env.getContext()).getClientMutationId();
          } else if (env.getContext() instanceof GraphQLContext) {
            return ((GraphQLContext) env.getContext()).get(CLIENT_MUTATION_ID);
          } else {
            return simpleResolver.get(env);
          }
        };
    buildContext.codeRegistry.dataFetcher(
        coordinates(payloadTypeName, CLIENT_MUTATION_ID), clientMutationIdResolver);

    final GraphQLFieldDefinition relayMutation =
        newFieldDefinition()
            .name(mutation.getName())
            .type(outputType)
            .argument(newArgument().name("input").type(new GraphQLNonNull(inputObjectType)))
            .build();

    DataFetcher<?> wrappedResolver =
        env -> {
          DataFetchingEnvironment innerEnv = new RelayDataFetchingEnvironmentDecorator(env);
          if (env.getContext() instanceof ContextWrapper) {
            ContextWrapper context = env.getContext();
            context.setClientMutationId(innerEnv.getArgument(CLIENT_MUTATION_ID));
          } else if (env.getContext() instanceof GraphQLContext) {
            GraphQLContext context = env.getContext();
            context.put(CLIENT_MUTATION_ID, innerEnv.getArgument(CLIENT_MUTATION_ID));
          }
          return resolver.get(innerEnv);
        };
    buildContext.codeRegistry.dataFetcher(
        coordinates(parentType, relayMutation.getName()), wrappedResolver);

    return relayMutation;
  }

  /**
   * Creates a generic resolver for the given operation.
   *
   * @implSpec This resolver simply invokes {@link
   *     OperationExecutor#execute(DataFetchingEnvironment)}
   * @param operation The operation for which the resolver is being created
   * @param buildContext The shared context containing all the global information needed for mapping
   * @return The resolver for the given operation
   */
  private DataFetcher createResolver(Operation operation, BuildContext buildContext) {
    Stream<AnnotatedType> inputTypes =
        operation.getArguments().stream()
            .filter(OperationArgument::isMappable)
            .map(OperationArgument::getJavaType);
    ValueMapper valueMapper = buildContext.createValueMapper(inputTypes);

    if (operation.isBatched()) {
      return (BatchedDataFetcher)
          environment ->
              new OperationExecutor(
                      operation,
                      valueMapper,
                      buildContext.globalEnvironment,
                      buildContext.interceptorFactory)
                  .execute(environment);
    }
    return new OperationExecutor(
            operation, valueMapper, buildContext.globalEnvironment, buildContext.interceptorFactory)
        ::execute;
  }

  /**
   * Creates a resolver for the <em>node</em> query as defined by the Relay GraphQL spec.
   *
   * <p>This query only takes a singe argument called "id" of type String, and returns the object
   * implementing the <em>Node</em> interface to which the given id corresponds.
   *
   * @param nodeQueriesByType A map of all queries whose return types implement the <em>Node</em>
   *     interface, keyed by their corresponding GraphQL type name
   * @param relay Relay helper
   * @return The node query resolver
   */
  private DataFetcher createNodeResolver(Map<String, String> nodeQueriesByType, Relay relay) {
    return env -> {
      String typeName;
      try {
        typeName = relay.fromGlobalId(env.getArgument(GraphQLId.RELAY_ID_FIELD_NAME)).getType();
      } catch (Exception e) {
        throw new IllegalArgumentException(
            env.getArgument(GraphQLId.RELAY_ID_FIELD_NAME) + " is not a valid Relay node ID");
      }
      if (!nodeQueriesByType.containsKey(typeName)) {
        throw new IllegalArgumentException(
            typeName + " is not a Relay node type or no registered query can fetch it by ID");
      }
      final GraphQLObjectType queryRoot = env.getGraphQLSchema().getQueryType();
      final GraphQLFieldDefinition nodeQueryForType =
          queryRoot.getFieldDefinition(nodeQueriesByType.get(typeName));

      return env.getGraphQLSchema()
          .getCodeRegistry()
          .getDataFetcher(queryRoot, nodeQueryForType)
          .get(env);
    };
  }

  private Map<String, String> getNodeQueriesByType(
      List<Operation> queries,
      List<GraphQLFieldDefinition> graphQlQueries,
      TypeRegistry typeRegistry,
      GraphQLInterfaceType node,
      BuildContext buildContext) {

    Map<String, String> nodeQueriesByType = new HashMap<>();

    for (int i = 0; i < queries.size(); i++) {
      Operation query = queries.get(i);
      GraphQLFieldDefinition graphQlQuery = graphQlQueries.get(i);

      if (graphQlQuery.getArgument(GraphQLId.RELAY_ID_FIELD_NAME) != null
          && GraphQLUtils.isRelayId(graphQlQuery.getArgument(GraphQLId.RELAY_ID_FIELD_NAME))
          && query.getResolver(GraphQLId.RELAY_ID_FIELD_NAME) != null) {

        GraphQLType unwrappedQueryType = GraphQLUtils.unwrapNonNull(graphQlQuery.getType());
        unwrappedQueryType = buildContext.typeCache.resolveType(unwrappedQueryType.getName());
        if (unwrappedQueryType instanceof GraphQLObjectType
            && ((GraphQLObjectType) unwrappedQueryType).getInterfaces().contains(node)) {
          nodeQueriesByType.put(unwrappedQueryType.getName(), query.getName());
        } else if (unwrappedQueryType instanceof GraphQLInterfaceType) {
          typeRegistry.getOutputTypes(unwrappedQueryType.getName()).stream()
              .map(MappedType::getAsObjectType)
              .filter(implementation -> implementation.getInterfaces().contains(node))
              .forEach(
                  nodeType ->
                      nodeQueriesByType.putIfAbsent(
                          nodeType.getName(),
                          query.getName())); // never override more precise resolvers
        } else if (unwrappedQueryType instanceof GraphQLUnionType) {
          typeRegistry.getOutputTypes(unwrappedQueryType.getName()).stream()
              .map(MappedType::getAsObjectType)
              .filter(implementation -> implementation.getInterfaces().contains(node))
              .filter(Directives::isMappedType)
              // only register the possible types that can actually be returned from the primary
              // resolver
              // for interface-unions it is all the possible types but, for inline unions, only one
              // (right?) possible type can match
              .filter(
                  implementation ->
                      GenericTypeReflector.isSuperType(
                          query
                              .getResolver(GraphQLId.RELAY_ID_FIELD_NAME)
                              .getReturnType()
                              .getType(),
                          Directives.getMappedType(implementation).getType()))
              .forEach(
                  nodeType ->
                      nodeQueriesByType.putIfAbsent(
                          nodeType.getName(),
                          query.getName())); // never override more precise resolvers
        }
      }
    }
    return nodeQueriesByType;
  }

  private void log(Validator.ValidationResult result) {
    if (!result.isValid()) {
      log.warn(result.getMessage());
    }
  }

  private void validateConnectionSpecCompliance(
      String operationName, List<GraphQLArgument> arguments, Relay relay) {
    String errorMessageTemplate =
        "Operation '"
            + operationName
            + "' is incompatible with the Relay Connection spec due to %s. "
            + "If this is intentional, disable strict compliance checking. "
            + "For details and solutions see "
            + Urls.Errors.RELAY_CONNECTION_SPEC_VIOLATION;

    boolean forwardPageSupported =
        relay.getForwardPaginationConnectionFieldArguments().stream()
            .allMatch(
                specArg ->
                    arguments.stream().anyMatch(arg -> arg.getName().equals(specArg.getName())));
    boolean backwardPageSupported =
        relay.getBackwardPaginationConnectionFieldArguments().stream()
            .allMatch(
                specArg ->
                    arguments.stream().anyMatch(arg -> arg.getName().equals(specArg.getName())));

    if (!forwardPageSupported && !backwardPageSupported) {
      throw new MappingException(String.format(errorMessageTemplate, "required arguments missing"));
    }

    if (relay.getConnectionFieldArguments().stream()
        .anyMatch(
            specArg ->
                arguments.stream()
                    .anyMatch(
                        arg ->
                            arg.getName().equals(specArg.getName())
                                && !GraphQLUtils.unwrap(arg.getType())
                                    .getName()
                                    .equals(specArg.getType().getName())))) {
      throw new MappingException(String.format(errorMessageTemplate, "argument type mismatch"));
    }
  }

  /**
   * Fetches all the mapped GraphQL fields representing top-level queries, ready to be attached to
   * the root query type.
   *
   * @return A list of GraphQL fields representing top-level queries
   */
  public List<GraphQLFieldDefinition> getQueries() {
    return queries;
  }

  /**
   * Fetches all the mapped GraphQL fields representing mutations, ready to be attached to the root
   * mutation type.
   *
   * @return A list of GraphQL fields representing mutations
   */
  public List<GraphQLFieldDefinition> getMutations() {
    return mutations;
  }

  public List<GraphQLFieldDefinition> getSubscriptions() {
    return subscriptions;
  }

  public List<GraphQLDirective> getDirectives() {
    return directives;
  }

  private String getFieldSignature(GraphQLInputObjectType type, GraphQLInputObjectField field) {
    String typeName = type.getName();
    if (typeName.endsWith(TypeInfoGenerator.INPUT_SUFFIX)) {
      typeName = typeName.substring(0, typeName.length() - TypeInfoGenerator.INPUT_SUFFIX.length());
    }
    return typeName + "." + field.getName();
  }

  private String getFieldSignature(GraphQLType type, GraphQLFieldDefinition fd) {
    return type.getName() + "." + fd.getName();
  }

  private String getInputFieldSignature(
      GraphQLInputObjectType type, GraphQLInputObjectField field) {
    // We want the signatures of Graphql input/type structures have the same format
    return type.getName() + "." + field.getName();
  }

  public GraphQLType getDynamicFieldType(
      GraphQLType type, GraphQLFieldDefinition fd, GraphQLOutputType defaultValue) {
    return getDynamicFieldType(getFieldSignature(type, fd), defaultValue);
  }

  public GraphQLInputType updateDynamicFieldType(
      GraphQLInputObjectType type, GraphQLInputObjectField fd, Object defaultValue) {

    String fieldSignature = getInputFieldSignature(type, fd);

    if (dynamicFieldTypes.containsKey(fieldSignature)) {
      return (GraphQLInputType) dynamicFieldTypes.get(fieldSignature);
    }
    GraphQLInputType graphQLInputType = toGraphQLInputType(defaultValue);
    dynamicFieldTypes.put(fieldSignature, graphQLInputType);

    GraphQLOutputType graphQLType = toGraphQLType(defaultValue);
    fieldSignature = getFieldSignature(type, fd);
    dynamicFieldTypes.put(fieldSignature, graphQLType);

    return graphQLInputType;
  }

  public GraphQLType getDynamicFieldType(GraphQLObjectType type, GraphQLFieldDefinition field) {
    return getDynamicFieldType(getFieldSignature(type, field), null);
  }

  public GraphQLType getDynamicFieldType(
      GraphQLInputObjectType type, GraphQLFieldDefinition field) {
    return getDynamicFieldType(getFieldSignature(type, field), null);
  }

  public GraphQLType getDynamicFieldType(String fieldSignature, GraphQLType defaultType) {
    return dynamicFieldTypes.getOrDefault(fieldSignature, defaultType);
  }
}
