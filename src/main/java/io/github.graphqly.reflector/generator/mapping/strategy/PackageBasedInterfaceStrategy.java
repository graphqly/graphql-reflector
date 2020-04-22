package io.github.graphqly.reflector.generator.mapping.strategy;

import io.github.graphqly.reflector.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

/** @author Bojan Tomic (kaqqao) */
public class PackageBasedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

  private final String packageName;

  public PackageBasedInterfaceStrategy(String packageName, boolean mapClasses) {
    super(mapClasses);
    this.packageName = packageName;
  }

  @Override
  public boolean supportsInterface(AnnotatedType interfase) {
    return ClassUtils.isSubPackage(
        ClassUtils.getRawType(interfase.getType()).getPackage(), packageName);
  }
}
