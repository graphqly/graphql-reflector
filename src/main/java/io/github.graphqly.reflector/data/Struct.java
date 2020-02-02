package io.github.graphqly.reflector.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

import static io.github.graphqly.reflector.data.JsonUtils.*;

public class Struct extends HashMap<Object, Object> {

  public Struct() {}

  public Struct(Object... values) {
    this.update(values);
  }

  private Struct put(io.github.graphqly.reflector.data.KeyValue keyValue) {
    this.put(keyValue.key, keyValue.value);
    return this;
  }

  private Struct put(StructEntry entry) {
    this.put(entry.key, entry.value);
    return this;
  }

  private Struct put(JsonObject jsonObject) {

    jsonObject
        .entrySet()
        .forEach(
            entry -> {
              String key = entry.getKey();
              JsonElement value = entry.getValue();
              if (value.isJsonPrimitive()) {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                Struct.this.put(key, unwrapJsonPrimitive(primitive));
                return;
              }
              if (value.isJsonObject()) {
                Struct.this.put(key, new Struct(value.getAsJsonObject()));
                return;
              }
              if (value.isJsonArray()) {
                Struct.this.put(key, unwrapJsonArray(value.getAsJsonArray()));
              }
            });
    return this;
  }

  private Struct put(Map<Object, Object> dict) {
    dict.entrySet().forEach(entry -> Struct.this.put(entry.getKey(), entry.getValue()));
    return this;
  }

  public Struct update(Object... values) {
    for (int i = 0; i < values.length; i++) {
      if (values[i] instanceof Map) {
        this.put((Map<Object, Object>) values[i]);
        continue;
      }
      if (values[i] instanceof StructEntry) {
        this.put((StructEntry) values[i]);
        continue;
      }
      if (values[i] instanceof JsonObject) {
        this.put((JsonObject) values[i]);
        continue;
      }
      if (values[i] instanceof io.github.graphqly.reflector.data.KeyValue) {
        this.put((io.github.graphqly.reflector.data.KeyValue) values[i]);
        continue;
      }

      this.put(jsonize(values[i]));
      //      if (i + 1 >= values.length) {
      //        break;
      //      }
      //      String key = values[i].toString();
      //      Object value = values[++i];
      //      this.put(key, value);
    }

    return this;
  }

  public JsonObject asJson() {
    return jsonize(this);
  }

  public <T> T mapTo(Class<T> prototype) {
    return JsonUtils.createObject(prototype, asJson());
  }

  public Struct entry(Object key, Object value) {
    this.put(key.toString(), value);
    return this;
  }

  public StructEntry key(Object key) {
    return new StructEntry(key.toString());
  }

  class StructEntry {
    public String key;
    public Object value;

    public StructEntry(String key) {
      this.key = key;
    }

    public Struct value(Object value) {
      this.value = value;
      return Struct.this.update(key, value);
    }

    public JsonObject asJson() {
      return JsonUtils.jsonize(this);
    }
  }
}
