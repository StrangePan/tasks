package tasks.model;

import omnia.cli.out.Output;
import omnia.data.structure.Set;

public interface Task {

  TaskId id();

  String label();

  Status status();

  boolean isUnblocked();

  Set<? extends Task> blockingTasks();

  Set<? extends Task> blockedTasks();

  Output render();

  enum Status {
    OPEN,
    COMPLETED,
    STARTED;

    public boolean isOpen() {
      return this.equals(OPEN);
    }

    public boolean isCompleted() {
      return this.equals(COMPLETED);
    }

    public boolean isStarted() {
      return this.equals(STARTED);
    }
  }
}
