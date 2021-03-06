package io.github.graphqly.reflector.metadata.strategy.type;

import io.github.graphqly.reflector.metadata.messages.MessageBundle;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedType;

/** @author Bojan Tomic (kaqqao) */
public interface TypeInfoGenerator {

  String INPUT_SUFFIX = "Request";
  String SCALAR_SUFFIX = "Scalar";

  String generateTypeName(AnnotatedType type, MessageBundle messageBundle);

  String generateTypeDescription(AnnotatedType type, MessageBundle messageBundle);

  default String generateInputTypeName(AnnotatedType type, MessageBundle messageBundle) {
    String typeName = generateTypeName(type, messageBundle);
    return typeName.endsWith(INPUT_SUFFIX) ? typeName : typeName + INPUT_SUFFIX;
  }

  default String generateInputTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
    return generateTypeDescription(type, messageBundle);
  }

  default String generateScalarTypeName(AnnotatedType type, MessageBundle messageBundle) {
    String scalarType = generateTypeName(type, messageBundle);
    return scalarType.endsWith(SCALAR_SUFFIX) ? scalarType : scalarType + SCALAR_SUFFIX;
  }

  default String generateScalarTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
    return generateTypeDescription(type, messageBundle);
  }

  default String generateDirectiveTypeName(AnnotatedType type, MessageBundle messageBundle) {
    return Introspector.decapitalize(generateTypeName(type, messageBundle));
  }

  default String generateDirectiveTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
    return generateTypeDescription(type, messageBundle);
  }
}
