package tasks.model.impl;

import java.util.Objects;

final class TaskId {

  private final long id;

  TaskId(long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TaskId && ((TaskId) other).id == id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  long asLong() {
    return id;
  }
}
