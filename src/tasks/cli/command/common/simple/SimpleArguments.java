package tasks.cli.command.common.simple;

import omnia.data.structure.List;
import tasks.model.ObservableTask;

public abstract class SimpleArguments {
  private final List<ObservableTask> tasks;

  protected SimpleArguments(List<ObservableTask> tasks) {
    this.tasks = tasks;
  }

  /** The list of tasks parsed from the command line. */
  protected List<ObservableTask> tasks() {
    return tasks;
  }
}
