package tasks.model;

public interface TaskBuilder {

  TaskBuilder setCompleted(boolean completed);

  TaskBuilder setBlockingTasks(Iterable<? extends Task> task);

  TaskBuilder addBlockingTask(Task task);

  TaskBuilder setBlockedTasks(Iterable<? extends Task> task);

  TaskBuilder addBlockedTask(Task task);
}
