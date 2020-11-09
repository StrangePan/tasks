package tasks.cli.feature.remove;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.ObservableTask;

/** Model for parsed Remove command arguments. */
public final class RemoveArguments extends SimpleArguments {
  RemoveArguments(List<ObservableTask> tasks) {
    super(tasks);
  }

  /** The tasks to remove from the store. */
  @Override
  public List<ObservableTask> tasks() {
    return super.tasks();
  }
}
