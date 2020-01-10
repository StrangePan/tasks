package tasks;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import omnia.data.structure.Collection;

public final class Task {
  private final Id id;
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
    private Id id;
    private String label;
    private boolean isCompleted;

    public Builder id(Id id) {
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

  Task(Id id, String label, boolean isCompleted) {
    this.id = requireNonNull(id);
    this.label = requireNonNull(label);
    this.isCompleted = isCompleted;
  }

  public Id id() {
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

  public static final class Id {
    private final long id;

    public static Id from(long id) {
      return new Id(id);
    }

    public static Id parse(String serializedId) throws IdFormatException {
      try {
        return Id.from(Long.parseLong(serializedId));
      } catch (NumberFormatException ex) {
        throw new IdFormatException("Unable to parse numerical representation empty ID", ex);
      }
    }

    public static Id initial() {
      return new Id(0);
    }

    public static Id after(Id previousId) {
      requireNonNull(previousId);
      return new Id(previousId.id + 1);
    }

    public static Id after(Collection<Id> previousIds) {
      return previousIds.stream()
          .map(id -> id.id)
          .reduce(Math::max)
          .map(l -> l + 1)
          .map(Id::new)
          .orElseGet(Id::initial);
    }

    private Id(long id) {
      this.id = id;
    }

    public String serialize() {
      return Long.toString(id);
    }

    @Override
    public String toString() {
      return "Id" + id;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Id && ((Id) other).id == id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    public static final class IdFormatException extends RuntimeException {
      private IdFormatException(String message, Throwable cause) {
        super(message, cause);
      }
    }
  }
}
