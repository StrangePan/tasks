package tasks.model.impl;

import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;

public class CyclicalDependencyException extends IllegalStateException {
  private final List<Object> cycle;

  CyclicalDependencyException(String msg, List<?> cycle) {
    super(msg);
    this.cycle = ImmutableList.copyOf(cycle);
  }

  public List<Object> cycle() {
    return cycle;
  }
}
