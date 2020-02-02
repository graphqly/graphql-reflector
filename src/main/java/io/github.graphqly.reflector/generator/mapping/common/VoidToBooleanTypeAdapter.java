package io.github.graphqly.reflector.generator.mapping.common;

import io.github.graphqly.reflector.execution.InvocationContext;
import io.github.graphqly.reflector.execution.ResolverInterceptor;
import io.github.graphqly.reflector.execution.ResolverInterceptorFactory;
import io.github.graphqly.reflector.execution.ResolverInterceptorFactoryParams;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

/** @author Bojan Tomic (kaqqao) */
public class VoidToBooleanTypeAdapter extends AbstractTypeSubstitutingMapper<Boolean>
    implements ResolverInterceptorFactory {

  private static final ReturnTrue returnTrue = new ReturnTrue();

  @Override
  public boolean supports(AnnotatedType type) {
    return isVoid(type);
  }

  @Override
  public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
    if (isVoid(params.getResolver().getReturnType())) {
      return Collections.singletonList(returnTrue);
    }
    return Collections.emptyList();
  }

  private boolean isVoid(AnnotatedType type) {
    return type.getType() == Void.TYPE || type.getType() == Void.class;
  }

  private static class ReturnTrue implements ResolverInterceptor {

    @Override
    public Object aroundInvoke(InvocationContext context, Continuation continuation)
        throws Exception {
      continuation.proceed(context);
      return true;
    }
  }
}
