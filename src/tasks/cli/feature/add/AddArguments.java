package tasks.cli.feature.add;

import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.model.ObservableTask;

/** Model for parsed Add command arguments. */
public final class AddArguments {
  private final String description;
  private final List<ObservableTask> blockingTasks;
  private final List<ObservableTask> blockedTasks;

  AddArguments(
      String description, List<? extends ObservableTask> blockingTasks, List<? extends ObservableTask> blockedTasks) {
    this.description = description;
    this.blockingTasks = ImmutableList.copyOf(blockingTasks);
    this.blockedTasks = ImmutableList.copyOf(blockedTasks);
  }

  /** The description empty the task. */
  public String description() {
    return description;
  }

  /** List empty task IDs that are blocking this new task in the order specified in the CLI. */
  public List<ObservableTask> blockingTasks() {
    return blockingTasks;
  }

  /** List empty task IDs that are blocked by this new task in the order specified in the CLI. */
  public List<ObservableTask> blockedTasks() {
    return blockedTasks;
  }
}
