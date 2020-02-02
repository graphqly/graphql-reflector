package io.github.graphqly.reflector.execution.complexity;

@FunctionalInterface
public interface ComplexityFunction {

  int getComplexity(ResolvedField node, int childScore);
}
