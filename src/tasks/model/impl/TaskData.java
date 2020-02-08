package tasks.model.impl;

import static java.util.Objects.requireNonNull;

final class TaskData {

  private final boolean completed;
  private final String label;

  TaskData(boolean completed, String label) {
    this.completed = completed;
    this.label = requireNonNull(label);
  }

  boolean completed() {
    return completed;
  }

  String label() {
    return label;
  }
}
