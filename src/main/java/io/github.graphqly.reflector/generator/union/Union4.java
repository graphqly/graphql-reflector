package io.github.graphqly.reflector.generator.union;

import java.lang.reflect.AnnotatedType;
import java.util.List;

/** @author Bojan Tomic (kaqqao) */
public class Union4<T1, T2, T3, T4> extends Union {

  public Union4(String name, String description, List<AnnotatedType> javaTypes) {
    super(name, description, javaTypes);
  }
}
