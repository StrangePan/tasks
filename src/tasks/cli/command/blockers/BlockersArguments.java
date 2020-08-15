package tasks.cli.command.blockers;

import omnia.data.structure.List;
import tasks.model.Task;

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

  public Task targetTask() {
    return targetTask;
  }

  public List<Task> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  public List<Task> blockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  public boolean clearAllBlockers() {
    return clearAllBlockers;
  }

}
