package io.github.graphqly.reflector.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.*;

public class BaseTypeSynonymComparator implements Comparator<AnnotatedType> {

  private final Set<Type> synonymGroup;

  public BaseTypeSynonymComparator(Type... synonymGroup) {
    this.synonymGroup = new HashSet<>();
    Collections.addAll(this.synonymGroup, synonymGroup);
  }

  @Override
  public int compare(AnnotatedType o1, AnnotatedType o2) {
    if (synonymGroup.contains(o1.getType())
        && synonymGroup.contains(o2.getType())
        && Arrays.stream(o1.getAnnotations())
            .allMatch(ann -> Arrays.asList(o2.getAnnotations()).contains(ann))) {
      return 0;
    }
    return -1;
  }
}
