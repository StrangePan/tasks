package tasks.cli.command.amend;

import java.util.Optional;
import omnia.data.structure.List;
import tasks.model.Task;

public final class AmendArguments {
  private final Task targetTask;
  private final Optional<String> description;
  private final List<Task> blockingTasks;
  private final List<Task> blockingTasksToAdd;
  private final List<Task> blockingTasksToRemove;
  private final List<Task> blockedTasks;
  private final List<Task> blockedTasksToAdd;
  private final List<Task> blockedTasksToRemove;

  AmendArguments(
      Task targetTask,
      Optional<String> description,
      List<Task> blockingTasks,
      List<Task> blockingTasksToAdd,
      List<Task> blockingTasksToRemove,
      List<Task> blockedTasks,
      List<Task> blockedTasksToAdd,
      List<Task> blockedTasksToRemove) {
    this.targetTask = targetTask;
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockingTasksToAdd = blockingTasksToAdd;
    this.blockingTasksToRemove = blockingTasksToRemove;
    this.blockedTasks = blockedTasks;
    this.blockedTasksToAdd = blockedTasksToAdd;
    this.blockedTasksToRemove = blockedTasksToRemove;
  }

  public Task targetTask() {
    return targetTask;
  }

  public Optional<String> description() {
    return description;
  }

  public List<Task> blockingTasks() {
    return blockingTasks;
  }

  public List<Task> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  public List<Task> blockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  public List<Task> blockedTasks() {
    return blockedTasks;
  }

  public List<Task> blockedTasksToAdd() {
    return blockedTasksToAdd;
  }

  public List<Task> blockedTasksToRemove() {
    return blockedTasksToRemove;
  }

}
