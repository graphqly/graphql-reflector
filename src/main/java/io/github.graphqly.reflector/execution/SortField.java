package io.github.graphqly.reflector.execution;

/** Created by bojan.tomic on 3/31/16. */
public class SortField {

  private final String name;
  private final Direction direction;

  public SortField(String name, Direction direction) {
    this.name = name;
    this.direction = direction;
  }

  public String getName() {
    return name;
  }

  public Direction getDirection() {
    return direction;
  }

  public enum Direction {
    ASC,
    DESC
  }
}
