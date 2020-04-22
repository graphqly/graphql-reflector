package io.github.graphqly.reflector.execution.relay.generic;

import graphql.relay.Edge;
import graphql.relay.PageInfo;
import io.github.graphqly.reflector.execution.relay.Page;

import java.util.List;

/** Created by bojan.tomic on 5/16/16. */
public class GenericPage<N> implements Page<N> {

  private final List<Edge<N>> edges;
  private final PageInfo pageInfo;

  @SuppressWarnings("WeakerAccess")
  public GenericPage(List<Edge<N>> edges, PageInfo pageInfo) {
    this.edges = edges;
    this.pageInfo = pageInfo;
  }

  @Override
  public List<Edge<N>> getEdges() {
    return edges;
  }

  @Override
  public PageInfo getPageInfo() {
    return pageInfo;
  }
}
