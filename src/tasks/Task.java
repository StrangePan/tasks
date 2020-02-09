package tasks;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import tasks.cli.CliTaskId;

public final class Task {
  private final CliTaskId id;
  private final String label;
  private final boolean isCompleted;

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return builder()
        .id(id())
        .label(label())
        .isCompleted(isCompleted());
  }

  public static final class Builder {
    private CliTaskId id;
    private String label;
    private boolean isCompleted;

    public Builder id(CliTaskId id) {
      this.id = id;
      return this;
    }

    public Builder label(String label) {
      this.label = label;
      return this;
    }

    public Builder isCompleted(boolean isCompleted) {
      this.isCompleted = isCompleted;
      return this;
    }

    public Task build() {
      return new Task(id, label, isCompleted);
    }
  }

  Task(CliTaskId id, String label, boolean isCompleted) {
    this.id = requireNonNull(id);
    this.label = requireNonNull(label);
    this.isCompleted = isCompleted;
  }

  public CliTaskId id() {
    return id;
  }

  public String label() {
    return label;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Task)) {
      return false;
    }
    Task otherTask = (Task) other;
    return Objects.equals(otherTask.id, id)
        && Objects.equals(otherTask.label, label)
        && Objects.equals(otherTask.isCompleted, isCompleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label, isCompleted);
  }

  @Override
  public String toString() {
    return id().toString()
        + ": "
        + label()
        + (isCompleted() ? " (completed)" : "");
  }

}
