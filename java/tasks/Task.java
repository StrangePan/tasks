package tasks;

import static omnia.data.stream.Collectors.toImmutableSet;

import omnia.data.structure.Collection;
import omnia.data.structure.immutable.ImmutableSet;

public interface Task {

  Id id();

  String name();

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
  }

  interface Builder {

    Builder id(Id id);

    Builder name(String name);

    Builder dependencies(Collection<Task> dependencies);

    Builder isCompleted(boolean isCompleted);

    Task build();
  }

  static Builder builder() {
    class DefaultTask implements Task {
      private final Id id;
      private final String name;
      private final ImmutableSet<Task> dependencies;
      private final boolean isCompleted;

      DefaultTask(Id id, String name, ImmutableSet<Task> dependencies, boolean isCompleted) {
        this.id = id;
        this.name = name;
        this.dependencies = dependencies;
        this.isCompleted = isCompleted;
      }

      @Override
      public Id id() {
        return id;
      }

      @Override
      public String name() {
        return name;
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
      Id id;
      String name;
      ImmutableSet<Task> dependencies;
      boolean isCompleted;

      @Override
      public DefaultBuilder id(Id id) {
        this.id = id;
        return this;
      }

      @Override
      public DefaultBuilder name(String name) {
        this.name = name;
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
        return new DefaultTask(id, name, dependencies, isCompleted);
      }
    }

    return new DefaultBuilder();
  }
}
