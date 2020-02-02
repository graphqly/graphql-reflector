package io.github.graphqly.reflector.metadata.strategy.value.gson;

import com.google.gson.*;
import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.generator.mapping.InputConverter;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

class ConvertingDeserializer implements JsonDeserializer {

  private final AnnotatedType detectedType;
  private final Type substituteType;
  private final InputConverter inputConverter;
  private final GlobalEnvironment environment;
  private final ValueMapper valueMapper;

  ConvertingDeserializer(
      AnnotatedType detectedType,
      Type substituteType,
      InputConverter inputConverter,
      GlobalEnvironment environment,
      Gson gson) {
    this.detectedType = detectedType;
    this.substituteType = substituteType;
    this.inputConverter = inputConverter;
    this.environment = environment;
    this.valueMapper = new GsonValueMapper(gson);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object deserialize(
      JsonElement jsonElement, Type type, JsonDeserializationContext deserializationContext)
      throws JsonParseException {
    Object substitute = deserializationContext.deserialize(jsonElement, substituteType);
    return inputConverter.convertInput(substitute, detectedType, environment, valueMapper);
  }
}
