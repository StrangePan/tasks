package tasks.model;

public interface TaskBuilder {

  TaskBuilder setStatus(Task.Status status);

  TaskBuilder setBlockingTasks(Iterable<? extends Task> task);

  TaskBuilder addBlockingTask(Task task);

  TaskBuilder setBlockedTasks(Iterable<? extends Task> task);

  TaskBuilder addBlockedTask(Task task);
}
