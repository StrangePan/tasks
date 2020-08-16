package tasks.model.impl;

import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;

public class CyclicalDependencyException extends IllegalStateException {
  private final List<Object> cycle;

  CyclicalDependencyException(String msg, List<?> cycle) {
    super(buildMessage(msg, ImmutableList.copyOf(cycle)));
    this.cycle = ImmutableList.copyOf(cycle);
  }

  private static String buildMessage(String prefix, List<?> cycle) {
    StringBuilder message = new StringBuilder().append(prefix).append("\n");
    message.append("->").append(cycle.itemAt(0)).append("\n");
    for (int i = 1; i < cycle.count(); i++) {
      message.append("| ").append(cycle.itemAt(i)).append("\n");
    }
    message.append("--").append(cycle.itemAt(0));
    return message.toString();
  }

  public List<Object> cycle() {
    return cycle;
  }
}
