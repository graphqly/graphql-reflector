package io.github.graphqly.reflector.data;

import com.google.gson.JsonObject;

class KeyValue {
  public String key;
  public Object value;

  public KeyValue(String key) {
    this.key = key;
  }

  public KeyValue value(Object value) {
    this.value = value;
    return this;
  }

  public JsonObject asJson() {
    return JsonUtils.jsonize(this);
  }
}
