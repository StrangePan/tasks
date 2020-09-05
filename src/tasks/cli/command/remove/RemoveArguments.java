package tasks.cli.command.remove;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.Task;

/** Model for parsed Remove command arguments. */
public final class RemoveArguments extends SimpleArguments {
  RemoveArguments(List<Task> tasks) {
    super(tasks);
  }

  /** The tasks to remove from the store. */
  @Override
  public List<Task> tasks() {
    return super.tasks();
  }
}
