package io.github.graphqly.reflector.execution;

import io.github.graphqly.reflector.metadata.Resolver;

public class ResolverInterceptorFactoryParams {

  private final Resolver resolver;

  ResolverInterceptorFactoryParams(Resolver resolver) {
    this.resolver = resolver;
  }

  public Resolver getResolver() {
    return resolver;
  }
}
