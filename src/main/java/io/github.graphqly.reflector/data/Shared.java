package io.github.graphqly.reflector.data;

import com.google.gson.JsonObject;

public class Shared {

  public static KeyValue key(String key) {
    return new KeyValue(key);
  }

  public static JsonObject json(Object... values) {
    return JsonUtils.jsonize(values);
  }

  public static <T> T struct(Class<T> prototype, Object... values) {
    return JsonUtils.createObject(prototype, values);
  }

  public static Struct map(Object... values) {
    return new Struct().update(values);
  }
}
