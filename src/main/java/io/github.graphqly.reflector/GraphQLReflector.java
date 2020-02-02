package io.github.graphqly.reflector;

import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.Map;

public abstract class GraphQLReflector {
  protected final AnnotatedType rootType;
  protected final Map<AnnotatedType, Object> defaultValues;

  public static class Builder {
    private AnnotatedType rootType = null;
    Map<AnnotatedType, Object> defaultValues = new HashMap<>();

    public GraphQLReflector build() {
      return fromClass(rootType, defaultValues);
    }

    public <T> Builder rootType(Class<T> rootType) {
      return rootType(GenericTypeReflector.annotate(rootType));
    }

    public Builder rootType(AnnotatedType rootType) {
      this.rootType = rootType;
      return this;
    }

    public <T> Builder mapDefault(Class<T> prototype, T value) {
      this.defaultValues.put(GenericTypeReflector.annotate(prototype), value);
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static <T> GraphQLReflector fromClass(Class<T> rootType) {
    return fromClass(rootType, new HashMap<>());
  }

  public static <T> GraphQLReflector fromClass(
      Class<T> rootType, Map<AnnotatedType, Object> defaultValues) {
    return fromClass(GenericTypeReflector.annotate(rootType), defaultValues);
  }

  public static GraphQLReflector fromClass(
      AnnotatedType rootType, Map<AnnotatedType, Object> defaultValues) {
    return new GraphQLReflectorImpl(rootType, defaultValues);
  }

  protected GraphQLReflector(AnnotatedType rootType) {
    this(rootType, new HashMap<>());
  }

  protected GraphQLReflector(AnnotatedType rootType, Map<AnnotatedType, Object> defaultValues) {
    this.rootType = rootType;
    this.defaultValues = defaultValues;
    build(rootType, defaultValues);
  }

  protected abstract void build(AnnotatedType rootType, Map<AnnotatedType, Object> defaultValues);

  public abstract String inspectOperation(String name);

  public abstract String inspectSchema();
}
