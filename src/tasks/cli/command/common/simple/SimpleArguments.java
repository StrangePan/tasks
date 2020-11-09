package tasks.cli.command.common.simple;

import omnia.data.structure.List;
import tasks.model.Task;

public abstract class SimpleArguments {
  private final List<Task> tasks;

  protected SimpleArguments(List<Task> tasks) {
    this.tasks = tasks;
  }

  /** The list of tasks parsed from the command line. */
  protected List<Task> tasks() {
    return tasks;
  }
}
