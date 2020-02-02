package io.github.graphqly.reflector.metadata.strategy.query;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.metadata.Operation;
import io.github.graphqly.reflector.metadata.Resolver;

import java.lang.reflect.Type;
import java.util.List;

/** @author Bojan Tomic (kaqqao) */
public interface OperationBuilder {

  Operation buildQuery(Type context, List<Resolver> resolvers, GlobalEnvironment environment);

  Operation buildMutation(Type context, List<Resolver> resolvers, GlobalEnvironment environment);

  Operation buildSubscription(
      Type context, List<Resolver> resolvers, GlobalEnvironment environment);
}
