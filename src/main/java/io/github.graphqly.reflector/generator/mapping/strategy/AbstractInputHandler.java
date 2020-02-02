package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.graphqly.reflector.generator.BuildContext;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public interface AbstractInputHandler {

  Set<Type> findConstituentAbstractTypes(AnnotatedType javaType, BuildContext buildContext);

  List<Class<?>> findConcreteSubTypes(Class abstractType, BuildContext buildContext);
}
