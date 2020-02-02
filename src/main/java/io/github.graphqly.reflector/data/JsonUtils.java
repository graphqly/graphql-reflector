package io.github.graphqly.reflector.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class JsonUtils {

  private static Gson gson = new GsonBuilder().create();

  public static <T> T createObject(Class<T> prototype, JsonObject object) {
    return gson.fromJson(object, prototype);
  }

  public static <T> T createObject(Class<T> prototype, Object... values) {
    return gson.fromJson(jsonize(values), prototype);
  }

  public static void prettyPrintConsole(JsonObject jsonObject) {
    System.out.println(prettyPrint(jsonObject));
  }

  public static String prettyPrint(JsonObject jsonObject) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(jsonObject);
  }

  private static boolean hasMethod(Object instance, String method) {
    Method[] methods = instance.getClass().getMethods();
    for (Method m : methods) {
      if (m.getName().equalsIgnoreCase(method)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasBuildMethod(Object instance) {
    return hasMethod(instance, "build");
  }

  private static Object getOrBuild(Object instance) {
    Method[] methods = instance.getClass().getMethods();
    Method mtd = null;
    for (Method m : methods) {
      if (m.getName().equalsIgnoreCase("build")) {
        mtd = m;
        break;
      }
    }
    if (mtd != null) {
      try {
        return mtd.invoke(instance);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return instance;
  }

  private static JsonElement toJson(Object instance) {
    return gson.toJsonTree(getOrBuild(instance));
  }

  public static JsonObject jsonize(Object... values) {
    JsonObject json = new JsonObject();

    try {
      extendJsonObject(
          json,
          ConflictStrategy.PREFER_SECOND_OBJ,
          Arrays.stream(values)
              // patch processing for Entry instances
              .map(
                  value -> {
                    if (value instanceof KeyValue) {
                      KeyValue kv = (KeyValue) value;
                      JsonObject jsonObject = new JsonObject();

                      jsonObject.add(kv.key, toJson(kv.value));
                      return jsonObject;
                    }
                    return value;
                  })
              .map(JsonUtils::toJson)
              // drop non-mappable element
              .filter(jsonElement -> !jsonElement.isJsonPrimitive())
              .map(JsonElement::getAsJsonObject)
              .collect(Collectors.toList())
              .toArray(new JsonObject[0]));
    } catch (JsonConflictException e) {
      e.printStackTrace();
    }

    return json;
  }

  static void extendJsonObject(
      JsonObject destinationObject, ConflictStrategy conflictResolutionStrategy, JsonObject... objs)
      throws JsonConflictException {
    for (JsonObject obj : objs) {
      extendJsonObject(destinationObject, obj, conflictResolutionStrategy);
    }
  }

  static Map<String, Object> objectToMap(Object object) {
    return jsonToMap(jsonize(object));
  }

  static Map<String, Object> jsonToMap(JsonElement jsonElement) {
    return gson.fromJson(jsonElement, new TypeToken<HashMap<String, Object>>() {}.getType());
  }

  static void extendJsonObject(
      JsonObject leftObj, JsonObject rightObj, ConflictStrategy conflictStrategy)
      throws JsonConflictException {
    for (Map.Entry<String, JsonElement> rightEntry : rightObj.entrySet()) {
      String rightKey = rightEntry.getKey();
      JsonElement rightVal = rightEntry.getValue();
      if (leftObj.has(rightKey)) {
        // conflict
        JsonElement leftVal = leftObj.get(rightKey);
        if (leftVal.isJsonArray() && rightVal.isJsonArray()) {
          JsonArray leftArr = leftVal.getAsJsonArray();
          JsonArray rightArr = rightVal.getAsJsonArray();
          // concat the arrays -- there cannot be a conflict in an array, it's just a collection of
          // stuff
          for (int i = 0; i < rightArr.size(); i++) {
            leftArr.add(rightArr.get(i));
          }
        } else if (leftVal.isJsonObject() && rightVal.isJsonObject()) {
          // recursive merging
          extendJsonObject(leftVal.getAsJsonObject(), rightVal.getAsJsonObject(), conflictStrategy);
        } else { // not both arrays or objects, normal merge with conflict resolution
          handleMergeConflict(rightKey, leftObj, leftVal, rightVal, conflictStrategy);
        }
      } else { // no conflict, add to the object
        leftObj.add(rightKey, rightVal);
      }
    }
  }

  private static void handleMergeConflict(
      String key,
      JsonObject leftObj,
      JsonElement leftVal,
      JsonElement rightVal,
      ConflictStrategy conflictStrategy)
      throws JsonConflictException {
    {
      switch (conflictStrategy) {
        case PREFER_FIRST_OBJ:
          break; // do nothing, the right val gets thrown out
        case PREFER_SECOND_OBJ:
          leftObj.add(key, rightVal); // right side auto-wins, replace left val with its val
          break;
        case PREFER_NON_NULL:
          // check if right side is not null, and left side is null, in which case we use the right
          // val
          if (leftVal.isJsonNull() && !rightVal.isJsonNull()) {
            leftObj.add(key, rightVal);
          } // else do nothing since either the left value is non-null or the right value is null
          break;
        case THROW_EXCEPTION:
          throw new JsonConflictException(
              "Key "
                  + key
                  + " exists in both objects and the conflict resolution strategy is "
                  + conflictStrategy);
        default:
          throw new UnsupportedOperationException(
              "The conflict strategy " + conflictStrategy + " is unknown and cannot be processed");
      }
    }
  }

  public static Object unwrapJsonArray(final JsonArray jsonArray) {
    List<Object> result = new ArrayList<>();
    jsonArray.forEach(
        element -> {
          if (element.isJsonPrimitive()) {
            result.add(unwrapJsonPrimitive(element.getAsJsonPrimitive()));
            return;
          }

          if (element.isJsonObject()) {
            result.add(unwrapJsonObject(element.getAsJsonObject()));
          }

          if (element.isJsonArray()) {
            result.add(unwrapJsonArray(element.getAsJsonArray()));
          }
        });

    return result;
  }

  public static Object unwrapJsonObject(final JsonObject jsonObject) {
    return objectToMap(jsonObject);
  }

  public static Object unwrapJsonPrimitive(final JsonPrimitive primitive) {

    if (primitive == null) {
      return null;
    }

    if (primitive.isString()) {
      return primitive.getAsString();
    } else if (primitive.isBoolean()) {
      return primitive.getAsBoolean();
    } else if (primitive.isNumber()) {
      return unwrapNumber(primitive.getAsNumber());
    }

    return primitive;
  }

  private static boolean isPrimitiveNumber(final Number n) {
    return n instanceof Integer
        || n instanceof Double
        || n instanceof Long
        || n instanceof BigDecimal
        || n instanceof BigInteger;
  }

  private static Number unwrapNumber(final Number n) {
    Number unwrapped;

    if (!isPrimitiveNumber(n)) {
      BigDecimal bigDecimal = new BigDecimal(n.toString());
      if (bigDecimal.scale() <= 0) {
        if (bigDecimal.compareTo(new BigDecimal(Integer.MAX_VALUE)) <= 0) {
          unwrapped = bigDecimal.intValue();
        } else if (bigDecimal.compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0) {
          unwrapped = bigDecimal.longValue();
        } else {
          unwrapped = bigDecimal;
        }
      } else {
        final double doubleValue = bigDecimal.doubleValue();
        if (BigDecimal.valueOf(doubleValue).compareTo(bigDecimal) != 0) {
          unwrapped = bigDecimal;
        } else {
          unwrapped = doubleValue;
        }
      }
    } else {
      unwrapped = n;
    }
    return unwrapped;
  }

  public enum ConflictStrategy {
    THROW_EXCEPTION,
    PREFER_FIRST_OBJ,
    PREFER_SECOND_OBJ,
    PREFER_NON_NULL
  }

  public static class JsonConflictException extends Exception {
    public JsonConflictException(String message) {
      super(message);
    }
  }
}
