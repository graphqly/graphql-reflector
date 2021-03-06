package io.github.graphqly.reflector.metadata.execution;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;

public abstract class Executable<T extends AnnotatedElement & Member> {

  T delegate;

  public abstract Object execute(Object target, Object[] args)
      throws InvocationTargetException, IllegalAccessException;

  public abstract AnnotatedType getReturnType();

  /**
   * Returns the number of formal parameters this executable takes. May differ from {@code
   * getParameters().length}.
   *
   * @return The number of formal parameter this executable takes
   */
  public abstract int getParameterCount();

  public abstract AnnotatedType[] getAnnotatedParameterTypes();

  public abstract Parameter[] getParameters();

  public T getDelegate() {
    return delegate;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  /**
   * Two {@code Executable}s are considered equal either if their wrapped fields/methods are equal
   * or if one wraps a field and the other its corresponding getter/setter.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public boolean equals(Object that) {
    return this == that
        || (that instanceof Executable && ((Executable) that).delegate.equals(this.delegate));
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
