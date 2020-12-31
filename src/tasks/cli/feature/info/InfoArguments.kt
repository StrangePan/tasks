package tasks.cli.feature.info;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.Task;

/** Model for parsed Info command arguments. */
public final class InfoArguments extends SimpleArguments {
  InfoArguments(List<Task> tasks) {
    super(tasks);
  }

  /** The tasks for which to display information, in the order specified in the command line. */
  @Override
  public List<Task> tasks() {
    return super.tasks();
  }
}
