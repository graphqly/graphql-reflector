package io.github.graphqly.reflector.module.common.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.leangen.geantyref.GenericTypeReflector;
import io.github.graphqly.reflector.generator.BuildContext;
import io.github.graphqly.reflector.generator.OperationMapper;
import io.github.graphqly.reflector.generator.mapping.InputConverter;
import io.github.graphqly.reflector.generator.mapping.OutputConverter;
import io.github.graphqly.reflector.generator.mapping.TypeMapper;
import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class JsonNodeAdapter
    implements TypeMapper, InputConverter<JsonNode, Object>, OutputConverter<JsonNode, Object> {

  private static final Map<Class, JsonNodeDescriptor> mappings;

  static {
    Map<Class<?>, JsonNodeDescriptor<?, ?>> typeMapping = new HashMap<>();
    typeMapping.put(
        TextNode.class,
        new JsonNodeDescriptor<>(
            String.class, JsonNodeFactory.instance::textNode, TextNode::textValue));
    typeMapping.put(
        BooleanNode.class,
        new JsonNodeDescriptor<>(
            Boolean.class, JsonNodeFactory.instance::booleanNode, BooleanNode::booleanValue));
    typeMapping.put(
        BinaryNode.class,
        new JsonNodeDescriptor<>(byte[].class, BinaryNode::new, BinaryNode::binaryValue));
    typeMapping.put(
        DecimalNode.class,
        new JsonNodeDescriptor<>(BigDecimal.class, DecimalNode::new, DecimalNode::decimalValue));
    typeMapping.put(
        BigIntegerNode.class,
        new JsonNodeDescriptor<>(
            BigInteger.class, BigIntegerNode::new, BigIntegerNode::bigIntegerValue));
    typeMapping.put(
        IntNode.class, new JsonNodeDescriptor<>(Integer.class, IntNode::new, IntNode::intValue));
    typeMapping.put(
        DoubleNode.class,
        new JsonNodeDescriptor<>(Double.class, DoubleNode::new, DoubleNode::doubleValue));
    typeMapping.put(
        FloatNode.class,
        new JsonNodeDescriptor<>(Float.class, FloatNode::new, FloatNode::floatValue));
    typeMapping.put(
        ShortNode.class,
        new JsonNodeDescriptor<>(Short.class, ShortNode::new, ShortNode::shortValue));
    typeMapping.put(
        NumericNode.class,
        new JsonNodeDescriptor<>(BigDecimal.class, DecimalNode::new, DecimalNode::decimalValue));
    mappings = Collections.unmodifiableMap(typeMapping);
  }

  @Override
  @SuppressWarnings("unchecked")
  public JsonNode convertInput(
      Object substitute,
      AnnotatedType type,
      GlobalEnvironment environment,
      ValueMapper valueMapper) {
    return (JsonNode)
        mappings.get(ClassUtils.getRawType(type.getType())).deserializer.apply(substitute);
  }

  @Override
  public AnnotatedType getSubstituteType(AnnotatedType original) {
    return GenericTypeReflector.annotate(
        mappings.get(ClassUtils.getRawType(original.getType())).substituteClass,
        original.getAnnotations());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object convertOutput(
      JsonNode original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    return mappings.get(ClassUtils.getRawType(type.getType())).serializer.apply(original);
  }

  @Override
  public GraphQLOutputType toGraphQLType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return operationMapper.toGraphQLType(getSubstituteType(javaType), buildContext);
  }

  @Override
  public GraphQLInputType toGraphQLInputType(
      AnnotatedType javaType,
      OperationMapper operationMapper,
      Set<Class<? extends TypeMapper>> mappersToSkip,
      BuildContext buildContext) {
    return operationMapper.toGraphQLInputType(getSubstituteType(javaType), buildContext);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return mappings.containsKey(ClassUtils.getRawType(type.getType()));
  }

  private static class JsonNodeDescriptor<T extends JsonNode, S> {

    final Type substituteClass;
    final Function<S, T> deserializer;
    final Function<T, S> serializer;

    private JsonNodeDescriptor(
        Class<S> substituteClass, Function<S, T> deserializer, Function<T, S> serializer) {
      this.substituteClass = substituteClass;
      this.deserializer = deserializer;
      this.serializer = serializer;
    }
  }
}
