package tasks.model;

import omnia.cli.out.Output;
import omnia.data.structure.Set;

public interface Task {

  TaskId id();

  String label();

  boolean isCompleted();

  boolean isUnblocked();

  Set<? extends Task> blockingTasks();

  Set<? extends Task> blockedTasks();

  Output render();
}
