package io.github.graphqly.reflector.generator;

import io.github.graphqly.reflector.metadata.messages.MessageBundle;
import io.github.graphqly.reflector.metadata.strategy.type.TypeInfoGenerator;
import io.github.graphqly.reflector.util.GraphQLUtils;

/** @author Bojan Tomic (kaqqao) */
class RelayNodeTypeResolver extends DelegatingTypeResolver {

  RelayNodeTypeResolver(
      TypeRegistry typeRegistry, TypeInfoGenerator typeInfoGenerator, MessageBundle messageBundle) {
    super(GraphQLUtils.NODE, typeRegistry, typeInfoGenerator, messageBundle);
  }
}
