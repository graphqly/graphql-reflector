package io.github.graphqly.reflector.generator.mapping.common;

import graphql.relay.Edge;
import graphql.relay.Relay;
import graphql.schema.*;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.util.ClassUtils;
import io.leangen.geantyref.GenericTypeReflector;
import io.github.graphqly.reflector.execution.relay.Connection;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;
import io.github.graphqly.reflector.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** @author Bojan Tomic (kaqqao) */
public class PageMapper extends ObjectTypeMapper {

  @Override
  public GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    AnnotatedType edgeType =
        GenericTypeReflector.getTypeParameter(javaType, Connection.class.getTypeParameters()[0]);
    AnnotatedType nodeType =
        GenericTypeReflector.getTypeParameter(edgeType, Edge.class.getTypeParameters()[0]);
    String connectionName =
        buildContext.typeInfoGenerator.generateTypeName(nodeType, buildContext.messageBundle)
            + "Connection";
    if (buildContext.typeCache.contains(connectionName)) {
      return new GraphQLTypeReference(connectionName);
    }
    buildContext.typeCache.register(connectionName);
    GraphQLOutputType type = operationMapper.toGraphQLType(nodeType, buildContext);
    List<GraphQLFieldDefinition> edgeFields =
        getFields(type.getName() + "Edge", edgeType, buildContext, operationMapper).stream()
            .filter(field -> !GraphQLUtils.isRelayEdgeField(field))
            .collect(Collectors.toList());
    GraphQLObjectType edge = buildContext.relay.edgeType(type.getName(), type, null, edgeFields);
    List<GraphQLFieldDefinition> connectionFields =
        getFields(type.getName() + "Connection", javaType, buildContext, operationMapper).stream()
            .filter(field -> !GraphQLUtils.isRelayConnectionField(field))
            .collect(Collectors.toList());
    buildContext.typeRegistry.getDiscoveredTypes().add(Relay.pageInfoType);
    return buildContext.relay.connectionType(type.getName(), edge, connectionFields);
  }

  @Override
  public GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    throw new UnsupportedOperationException("Replay page type can not be used as input type");
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.isSuperClass(Connection.class, type);
  }
}
