package io.github.graphqly.reflector.execution.relay;

import graphql.relay.Edge;
import graphql.relay.PageInfo;

import java.util.List;

/** Created by bojan.tomic on 4/6/16. */
public interface Connection<E extends Edge> {

  List<E> getEdges();

  PageInfo getPageInfo();
}
