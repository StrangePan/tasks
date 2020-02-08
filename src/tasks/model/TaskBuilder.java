package tasks.model;

public interface TaskBuilder {

  TaskBuilder setCompleted(boolean completed);

  TaskBuilder addBlockingTask(Task task);

  TaskBuilder addBlockedTask(Task task);
}
