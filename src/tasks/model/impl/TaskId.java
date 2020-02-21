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

  @Override
  public String toString() {
    return Long.toString(asLong());
  }

  long asLong() {
    return id;
  }
}
