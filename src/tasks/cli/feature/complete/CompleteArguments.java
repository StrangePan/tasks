package tasks.cli.feature.complete;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.ObservableTask;

/** Model for parsed Complete command arguments. */
public final class CompleteArguments extends SimpleArguments {
  CompleteArguments(List<ObservableTask> tasks) {
    super(tasks);
  }

  /** The tasks to mark as completed, in the order specified in the command line. */
  @Override
  public List<ObservableTask> tasks() {
    return super.tasks();
  }
}
