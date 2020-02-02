package io.github.graphqly.reflector.util;

import io.github.graphqly.reflector.metadata.strategy.value.ValueMapperFactory;
import io.github.graphqly.reflector.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.github.graphqly.reflector.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import io.github.graphqly.reflector.module.Module;
import io.github.graphqly.reflector.module.common.gson.GsonModule;
import io.github.graphqly.reflector.module.common.jackson.JacksonModule;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;

import java.util.ArrayList;
import java.util.List;

public class Defaults {

  private static final IllegalStateException noJsonLib =
      new IllegalStateException(
          "No JSON deserialization library found on classpath. A compatible version of either Jackson or Gson "
              + "must be available or a custom ValueMapperFactory must be provided");

  private static JsonLib jsonLibrary() {
    for (JsonLib jsonLib : JsonLib.values()) {
      if (isAvailable(jsonLib)) {
        return jsonLib;
      }
    }
    throw noJsonLib;
  }

  private static boolean isAvailable(JsonLib jsonLib) {
    try {
      ClassUtils.forName(jsonLib.requiredClass);
      return true;
    } catch (ClassNotFoundException ge) {
      return false;
    }
  }

  public static ValueMapperFactory valueMapperFactory(TypeInfoGenerator typeInfoGenerator) {
    switch (jsonLibrary()) {
      case GSON:
        return GsonValueMapperFactory.builder().withTypeInfoGenerator(typeInfoGenerator).build();
      case JACKSON:
        return JacksonValueMapperFactory.builder().withTypeInfoGenerator(typeInfoGenerator).build();
      default:
        throw noJsonLib;
    }
  }

  public static List<Module> modules() {
    List<Module> defaultModules = new ArrayList<>(2);
    if (isAvailable(JsonLib.JACKSON)) {
      defaultModules.add(new JacksonModule());
    }
    if (isAvailable(JsonLib.GSON)) {
      defaultModules.add(new GsonModule());
    }
    return defaultModules;
  }

  private enum JsonLib {
    JACKSON("com.fasterxml.jackson.databind.ObjectMapper"),
    GSON("com.google.gson.Gson");

    public final String requiredClass;

    JsonLib(String requiredClass) {
      this.requiredClass = requiredClass;
    }
  }
}
