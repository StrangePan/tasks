package tasks.cli.feature.reword;

import tasks.model.ObservableTask;

public final class RewordArguments {
  private final ObservableTask targetTask;
  private final String description;

  RewordArguments(ObservableTask targetTask, String description) {
    this.targetTask = targetTask;
    this.description = description;
  }

  /** The target to reword. */
  public ObservableTask targetTask() {
    return targetTask;
  }

  /** The new description for the task. */
  public String description() {
    return description;
  }
}
