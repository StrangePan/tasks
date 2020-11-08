package tasks.cli.feature.reword;

import tasks.model.Task;

public final class RewordArguments {
  private final Task targetTask;
  private final String description;

  RewordArguments(Task targetTask, String description) {
    this.targetTask = targetTask;
    this.description = description;
  }

  /** The target to reword. */
  public Task targetTask() {
    return targetTask;
  }

  /** The new description for the task. */
  public String description() {
    return description;
  }
}
