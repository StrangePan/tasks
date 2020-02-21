package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

final class TaskData {

  private final boolean completed;
  private final String label;

  TaskData(boolean completed, String label) {
    this.completed = completed;
    this.label = requireNonNull(label);
  }

  boolean isCompleted() {
    return completed;
  }

  String label() {
    return label;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TaskData
        && ((TaskData) other).completed == this.completed
        && ((TaskData) other).label.equals(this.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(completed, label);
  }
}
