package io.github.graphqly.reflector.execution;

import java.util.List;

public interface ResolverInterceptorFactory {

  List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params);
}
