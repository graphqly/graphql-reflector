package io.github.graphqly.reflector.metadata.strategy.type;

import io.github.graphqly.reflector.metadata.exceptions.TypeMappingException;
import io.github.graphqly.reflector.util.ClassUtils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;

public class DefaultTypeTransformer implements TypeTransformer {

  private final AnnotatedType rawReplacement;
  private final AnnotatedType unboundedReplacement;

  public DefaultTypeTransformer(boolean replaceRaw, boolean replaceUnbounded) {
    AnnotatedType replacement = GenericTypeReflector.annotate(Object.class);
    this.rawReplacement = replaceRaw ? replacement : null;
    this.unboundedReplacement = replaceUnbounded ? replacement : null;
  }

  public DefaultTypeTransformer(AnnotatedType rawReplacement, AnnotatedType unboundedReplacement) {
    this.rawReplacement = rawReplacement;
    this.unboundedReplacement = unboundedReplacement;
  }

  @Override
  public AnnotatedType transform(AnnotatedType type) throws TypeMappingException {
    type = ClassUtils.eraseBounds(type, unboundedReplacement);
    return ClassUtils.completeGenerics(type, rawReplacement);
  }
}
