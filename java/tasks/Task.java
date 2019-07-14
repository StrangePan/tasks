package tasks;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import omnia.data.structure.Collection;
import omnia.data.structure.immutable.ImmutableSet;

public interface Task {

  Id id();

  String label();

  Collection<Task> dependencies();

  boolean isCompleted();

  final class Id {
    private final long id;

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

    public static Id from(long id) {
      return new Id(id);
    }

    public static Id parse(String serializedId) throws IdFormatException {
      try {
        return Id.from(Long.parseLong(serializedId));
      } catch (NumberFormatException ex) {
        throw new IdFormatException("Unable to parse numerical representation of ID", ex);
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

    public static final class IdFormatException extends RuntimeException {
      private IdFormatException(String message, Throwable cause) {
        super(message, cause);
      }
    }
  }

  interface Builder {

    Builder id(Id id);

    Builder label(String name);

    Builder dependencies(Collection<Task> dependencies);

    Builder isCompleted(boolean isCompleted);

    Task build();
  }

  static Builder builder() {
    class DefaultTask implements Task {
      private final Id id;
      private final String label;
      private final ImmutableSet<Task> dependencies;
      private final boolean isCompleted;

      DefaultTask(Id id, String label, ImmutableSet<Task> dependencies, boolean isCompleted) {
        this.id = requireNonNull(id);
        this.label = requireNonNull(label);
        this.dependencies = requireNonNull(dependencies);
        this.isCompleted = isCompleted;
      }

      @Override
      public Id id() {
        return id;
      }

      @Override
      public String label() {
        return label;
      }

      @Override
      public Collection<Task> dependencies() {
        return dependencies;
      }

      @Override
      public boolean isCompleted() {
        return isCompleted;
      }

      @Override
      public boolean equals(Object other) {
        if (!(other instanceof DefaultTask)) {
          return false;
        }
        DefaultTask otherTask = (DefaultTask) other;
        return Objects.equals(otherTask.id, id)
            && Objects.equals(otherTask.label, label)
            && Objects.equals(otherTask.dependencies, dependencies)
            && Objects.equals(otherTask.isCompleted, isCompleted);
      }

      @Override
      public int hashCode() {
        return Objects.hash(id, label, dependencies, isCompleted);
      }

      @Override
      public String toString() {
        return id().toString()
            + ": "
            + label()
            + (dependencies().isPopulated() ? "[" + dependencies().count() + "]" : "");
      }
    }

    class DefaultBuilder implements Builder {
      private Id id;
      private String label;
      private ImmutableSet<Task> dependencies;
      private boolean isCompleted;

      @Override
      public DefaultBuilder id(Id id) {
        this.id = id;
        return this;
      }

      @Override
      public DefaultBuilder label(String label) {
        this.label = label;
        return this;
      }

      @Override
      public DefaultBuilder dependencies(Collection<Task> dependencies) {
        this.dependencies =
            ImmutableSet.<Task>builder().addAll(requireNonNull(dependencies)).build();
        return this;
      }

      @Override
      public DefaultBuilder isCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
        return this;
      }

      @Override
      public DefaultTask build() {
        return new DefaultTask(
            id,
            label,
            dependencies != null ? dependencies : ImmutableSet.<Task>builder().build(),
            isCompleted);
      }
    }

    return new DefaultBuilder();
  }

  static Builder buildUpon(Task other) {
    return builder()
        .id(other.id())
        .label(other.label())
        .dependencies(other.dependencies())
        .isCompleted(other.isCompleted());
  }
}
