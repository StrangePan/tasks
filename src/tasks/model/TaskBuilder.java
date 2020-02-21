package tasks.model;

public interface TaskBuilder {

  TaskBuilder setCompleted(boolean completed);

  TaskBuilder setBlockingTasks(Iterable<Task> task);

  TaskBuilder addBlockingTask(Task task);

  TaskBuilder setBlockedTasks(Iterable<Task> task);

  TaskBuilder addBlockedTask(Task task);
}
