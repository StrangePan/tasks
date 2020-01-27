package tasks.model.impl;

import static java.util.Objects.requireNonNull;

final class TaskData {

  private final String label;

  TaskData(String label) {
    this.label = requireNonNull(label);
  }

  String label() {
    return label;
  }
}
