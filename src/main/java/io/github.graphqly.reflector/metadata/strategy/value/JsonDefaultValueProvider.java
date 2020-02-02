package io.github.graphqly.reflector.metadata.strategy.value;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.github.graphqly.reflector.util.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;

/** @author Bojan Tomic (kaqqao) */
public class JsonDefaultValueProvider implements DefaultValueProvider {

  private static final Logger log = LoggerFactory.getLogger(JsonDefaultValueProvider.class);
  private final ValueMapper valueMapper;

  public JsonDefaultValueProvider(GlobalEnvironment environment) {
    this.valueMapper =
        Defaults.valueMapperFactory(new DefaultTypeInfoGenerator())
            .getValueMapper(Collections.emptyMap(), environment);
  }

  @Override
  public Object getDefaultValue(
      AnnotatedElement targetElement, AnnotatedType type, Object initialValue) {
    return valueMapper.fromString((String) initialValue, type);
  }
}
