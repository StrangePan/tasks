package tasks.cli.feature.stop;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.Task;

/** Model for parsed Stop command arguments. */
public final class StopArguments extends SimpleArguments {
  StopArguments(List<Task> tasks) {
    super(tasks);
  }

  /** The tasks to mark ask open, in the order specified in the command line. */
  @Override
  public List<Task> tasks() {
    return super.tasks();
  }
}
