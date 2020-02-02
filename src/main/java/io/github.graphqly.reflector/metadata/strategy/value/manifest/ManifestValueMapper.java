package io.github.graphqly.reflector.metadata.strategy.value.manifest;

import com.google.gson.Gson;
import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.gson.GsonValueMapper;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ManifestValueMapper extends GsonValueMapper {

  private final InstanceResolver instanceResolver;

  public ManifestValueMapper(Gson gson, InstanceResolver instanceResolver) {
    super(gson);
    if (instanceResolver == null) {
      instanceResolver = InstanceResolver.defaultInstance();
    }
    this.instanceResolver = instanceResolver;
  }

  public ManifestValueMapper(Gson gson) {
    this(gson, null);
  }

  @Override
  protected Object defaultValue(
      Class<?> type, Field field, AnnotatedType fieldType, GlobalEnvironment environment) {
    try {
      Object instance = this.instanceResolver.resolve(type);
      if (instance != null) return field.get(instance);
    } catch (Exception e) {
    }
    return super.defaultValue(type, field, fieldType, environment);
  }

  public abstract static class InstanceResolver {
    public static InstanceResolver defaultInstance() {
      return new DefaultInstanceResolver();
    }

    public abstract Object resolve(Class<?> type);

    public abstract <T> InstanceResolver map(Class<T> type, T instance);

    public abstract InstanceResolver map(AnnotatedType type, Object instance);
  }

  public static class DefaultInstanceResolver extends InstanceResolver {
    private Map<Class<?>, Object> instances = new HashMap<>();

    public <T> DefaultInstanceResolver map(Class<T> type, T instance) {
      instances.put(type, instance);
      return this;
    }

    @Override
    public InstanceResolver map(AnnotatedType type, Object instance) {
      Class claz = (Class) (type.getType());
      instances.put(claz, instance);
      return this;
    }

    @Override
    public Object resolve(Class<?> type) {
      return instances.computeIfAbsent(
          type,
          aClass -> {
            try {
              // TODO: please note this in documentation
              // Classes must contain default no-args constructor
              return aClass.newInstance();
            } catch (Exception e) {
              return null;
            }
          });
    }
  }
}
