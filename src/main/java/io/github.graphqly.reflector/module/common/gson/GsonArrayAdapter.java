package io.github.graphqly.reflector.module.common.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.generator.mapping.DelegatingOutputConverter;
import io.github.graphqly.reflector.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.github.graphqly.reflector.util.ClassUtils;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GsonArrayAdapter extends AbstractTypeSubstitutingMapper<List<JsonElement>>
    implements DelegatingOutputConverter<JsonArray, List> {

  private static final AnnotatedType JSON = GenericTypeReflector.annotate(JsonElement.class);

  @Override
  public List convertOutput(
      JsonArray original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
    List<Object> elements = new ArrayList<>(original.size());
    original.forEach(element -> elements.add(resolutionEnvironment.convertOutput(element, JSON)));
    return elements;
  }

  @Override
  public List<AnnotatedType> getDerivedTypes(AnnotatedType type) {
    return Collections.singletonList(JSON);
  }

  @Override
  public boolean supports(AnnotatedType type) {
    return ClassUtils.isSuperClass(JsonArray.class, type);
  }
}
