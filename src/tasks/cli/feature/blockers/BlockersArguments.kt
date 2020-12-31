package tasks.cli.feature.blockers;

import omnia.data.structure.List;
import tasks.model.Task;

/** Model for parsed Blockers command arguments. */
public final class BlockersArguments {
  private final Task targetTask;
  private final List<Task> blockingTasksToAdd;
  private final List<Task> blockingTasksToRemove;
  private final boolean clearAllBlockers;

  BlockersArguments(
      Task targetTask,
      List<Task> blockingTasksToAdd,
      List<Task> blockingTasksToRemove,
      boolean clearAllBlockers) {
    this.targetTask = targetTask;
    this.blockingTasksToAdd = blockingTasksToAdd;
    this.blockingTasksToRemove = blockingTasksToRemove;
    this.clearAllBlockers = clearAllBlockers;
  }

  /** The task whose blockers to modify. */
  public Task targetTask() {
    return targetTask;
  }

  /** The collection of tasks to add as blockers to {@link #targetTask()}. */
  public List<Task> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  /**
   * The collection of tasks to remove as blockers from {@link #targetTask()}. These tasks are in
   * the order defined in the command line.
   *
   * <p>This parameter is to be treated as mutually exclusive with the {@link #clearAllBlockers()}
   * flag. If {@link #clearAllBlockers()} is set, tasks listed here are redundant.</p>
   */
  public List<Task> blockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  /**
   * A flag indicating that all current blockers of {@link #targetTask()} should be removed. This
   * is to be applied before other {@link #blockingTasksToAdd()}.
   */
  public boolean clearAllBlockers() {
    return clearAllBlockers;
  }
}
