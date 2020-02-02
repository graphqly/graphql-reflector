package io.github.graphqly.reflector.metadata.strategy.query;

import io.github.graphqly.reflector.metadata.OperationArgument;

import java.util.List;

/** Created by bojan.tomic on 7/17/16. */
public interface ResolverArgumentBuilder {

  List<OperationArgument> buildResolverArguments(ArgumentBuilderParams params);
}
