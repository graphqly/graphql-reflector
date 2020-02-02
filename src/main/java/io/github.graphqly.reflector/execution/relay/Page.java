package io.github.graphqly.reflector.execution.relay;

import graphql.relay.Edge;
import graphql.relay.PageInfo;

import java.util.List;

/** Created by bojan.tomic on 4/6/16. */
public interface Page<N> extends Connection<Edge<N>> {
  List<Edge<N>> getEdges();

  PageInfo getPageInfo();
}
