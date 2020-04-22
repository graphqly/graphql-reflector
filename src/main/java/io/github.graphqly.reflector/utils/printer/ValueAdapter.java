package io.github.graphqly.reflector.utils.printer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ValueAdapter {
  private static Value fromJsonPrimtiveElement(JsonElement value) {

    JsonPrimitive primitive = value.getAsJsonPrimitive();

    if (primitive.isBoolean()) {
      return BooleanValue.newBooleanValue(primitive.getAsBoolean()).build();
    }

    if (primitive.isNumber()) {
      Number number = primitive.getAsNumber();
      boolean isIntegral =
          number instanceof BigInteger
              || number instanceof Long
              || number instanceof Integer
              || number instanceof Short
              || number instanceof Byte;
      if (isIntegral) {
        return IntValue.newIntValue(BigInteger.valueOf(number.longValue())).build();
      }
      return FloatValue.newFloatValue(BigDecimal.valueOf(number.floatValue())).build();
    }

    if (primitive.isString()) {
      return StringValue.newStringValue(primitive.getAsString()).build();
    }
    return null;
  }

  private static Value fromJsonElement(JsonElement value) {
    if (value.isJsonPrimitive()) {
      return fromJsonPrimtiveElement(value);
    }
    if (value.isJsonObject()) {
      return ObjectValue.newObjectValue()
          .objectFields(
              StreamSupport.stream(value.getAsJsonObject().entrySet().spliterator(), false)
                  .map(
                      entry ->
                          ObjectField.newObjectField()
                              .name(entry.getKey())
                              .value(fromJsonElement(entry.getValue()))
                              .build())
                  .collect(Collectors.toList()))
          .build();
    }
    if (value.isJsonArray()) {
      return ArrayValue.newArrayValue()
          .values(
              StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
                  .map(ValueAdapter::fromJsonElement)
                  .collect(Collectors.toList()))
          .build();
    }
    //    JsonPrimitive primitive = value.getAsJsonPrimitive();
    //
    //    if (primitive.isBoolean()) {
    //      return BooleanValue.newBooleanValue(primitive.getAsBoolean()).build();
    //    }
    //
    //    if (primitive.isNumber()) {
    //      Number number = primitive.getAsNumber();
    //      boolean isIntegral =
    //          number instanceof BigInteger
    //              || number instanceof Long
    //              || number instanceof Integer
    //              || number instanceof Short
    //              || number instanceof Byte;
    //      if (isIntegral) {
    //        return IntValue.newIntValue(BigInteger.valueOf(number.longValue())).build();
    //      }
    //      return FloatValue.newFloatValue(BigDecimal.valueOf(number.floatValue())).build();
    //    }
    //
    //    if (primitive.isString()) {
    //      return StringValue.newStringValue(primitive.getAsString()).build();
    //    }
    //
    //    if (primitive.isJsonArray()) {
    //      return ArrayValue.newArrayValue()
    //          .values(
    //              StreamSupport.stream(primitive.getAsJsonArray().spliterator(), false)
    //                  .map(ValueAdapter::fromJsonElement)
    //                  .collect(Collectors.toList()))
    //          .build();
    //    }
    //
    //    if (primitive.isJsonObject()) {
    //      return ObjectValue.newObjectValue()
    //          .objectFields(
    //              StreamSupport.stream(primitive.getAsJsonObject().entrySet().spliterator(),
    // false)
    //                  .map(
    //                      entry ->
    //                          ObjectField.newObjectField()
    //                              .name(entry.getKey())
    //                              .value(fromJsonElement(entry.getValue()))
    //                              .build())
    //                  .collect(Collectors.toList()))
    //          .build();
    //    }
    return null;
  }

  public static Value fromJson(JsonObject value) {
    return fromJsonElement(value);
  }
}
