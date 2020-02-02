package io.github.graphqly.reflector.metadata.strategy.value.manifest;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;
import io.github.graphqly.reflector.metadata.strategy.value.gson.GsonValueMapperFactory;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Map;

public class ManifestValueMapperFactory extends GsonValueMapperFactory {
  private ManifestValueMapper.InstanceResolver instanceResolver;

  public ManifestValueMapperFactory(ManifestValueMapper.InstanceResolver instanceResolver) {
    this.instanceResolver = instanceResolver;
  }

  public ManifestValueMapperFactory() {
    this(null);
  }

  public <T> ManifestValueMapperFactory map(Class<T> type, T instance) {
    if (instanceResolver == null) {
      instanceResolver = ManifestValueMapper.InstanceResolver.defaultInstance();
    }
    instanceResolver.map(type, instance);
    return this;
  }

  public ManifestValueMapperFactory map(AnnotatedType type, Object instance) {
    if (instanceResolver == null) {
      instanceResolver = ManifestValueMapper.InstanceResolver.defaultInstance();
    }
    Class claz = (Class) (type.getType());
    instanceResolver.map(claz, instance);
    return this;
  }

  @Override
  public ValueMapper getValueMapper(
      Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
    GsonBuilder gsonBuilder = initBuilder(concreteSubTypes, environment);
    //    gsonBuilder.setExclusionStrategies(new ManifestExclusionStrategy());
    return new ManifestValueMapper(gsonBuilder.create(), this.instanceResolver);
  }

  static class ManifestExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes field) {
      // Ignore fields with auto-generated types by Lombok
      return field.getDeclaredType().getTypeName().contains("$");
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return clazz.getCanonicalName().contains("$");
      //      return false;
    }
  }
}
