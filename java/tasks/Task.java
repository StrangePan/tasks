package tasks;

import static omnia.data.stream.Collectors.toImmutableSet;

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

    public long asLong() {
      return id;
    }

    public static Id from(long id) {
      return new Id(id);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Id && ((Id) other).id == id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
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
        this.id = id;
        this.label = label;
        this.dependencies = dependencies;
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
        this.dependencies = dependencies.stream().collect(toImmutableSet());
        return this;
      }

      @Override
      public DefaultBuilder isCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
        return this;
      }

      @Override
      public DefaultTask build() {
        return new DefaultTask(id, label, dependencies, isCompleted);
      }
    }

    return new DefaultBuilder();
  }
}
