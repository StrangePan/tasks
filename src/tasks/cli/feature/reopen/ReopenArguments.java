package tasks.cli.feature.reopen;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.ObservableTask;

/** Model for parsed Reopen command arguments. */
public final class ReopenArguments extends SimpleArguments {
  ReopenArguments(List<ObservableTask> tasks) {
    super(tasks);
  }

  /** The tasks to reopen, in the order specified in the command line. */
  @Override
  public List<ObservableTask> tasks() {
    return super.tasks();
  }
}
