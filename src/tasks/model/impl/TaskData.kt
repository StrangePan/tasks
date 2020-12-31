package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import tasks.model.Task;

final class TaskData {

  private final String label;
  private final Task.Status status;

  TaskData(String label, Task.Status status) {
    this.label = requireNonNull(label);
    this.status = requireNonNull(status);
  }

  String label() {
    return label;
  }

  Task.Status status() {
    return status;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TaskData
        && Objects.equals(this.label, ((TaskData) other).label)
        && Objects.equals(this.status, ((TaskData) other).status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, status);
  }
}
