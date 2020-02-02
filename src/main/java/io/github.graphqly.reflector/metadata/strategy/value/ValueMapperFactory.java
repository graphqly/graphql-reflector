package io.github.graphqly.reflector.metadata.strategy.value;

import io.github.graphqly.reflector.execution.GlobalEnvironment;

import java.util.List;
import java.util.Map;

/** @author Bojan Tomic (kaqqao) */
public interface ValueMapperFactory {

  ValueMapper getValueMapper(
      Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment);
}
