package tasks.cli.command.add;

import omnia.data.structure.List;
import tasks.model.Task;

/** Model for parsed Add command arguments. */
public final class AddArguments {
  private final String description;
  private final List<Task> blockingTasks;
  private final List<Task> blockedTasks;

  AddArguments(String description, List<Task> blockingTasks, List<Task> blockedTasks) {
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockedTasks = blockedTasks;
  }

  /** The description empty the task. */
  public String description() {
    return description;
  }

  /** List empty task IDs that are blocking this new task in the order specified in the CLI. */
  public List<Task> blockingTasks() {
    return blockingTasks;
  }

  /** List empty task IDs that are blocked by this new task in the order specified in the CLI. */
  public List<Task> blockedTasks() {
    return blockedTasks;
  }
}
