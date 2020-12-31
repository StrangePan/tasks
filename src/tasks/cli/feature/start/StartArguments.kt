package tasks.cli.feature.start;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.Task;

/** Model for parsed Start command arguments. */
public final class StartArguments extends SimpleArguments {
  StartArguments(List<Task> tasks) {
    super(tasks);
  }

  /** The tasks to mark as started, in the order specified in the command line. */
  @Override
  public List<Task> tasks() {
    return super.tasks();
  }
}
