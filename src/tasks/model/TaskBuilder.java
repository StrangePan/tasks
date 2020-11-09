package tasks.model;

public interface TaskBuilder {

  TaskBuilder setCompleted(boolean completed);

  TaskBuilder setBlockingTasks(Iterable<ObservableTask> task);

  TaskBuilder addBlockingTask(ObservableTask task);

  TaskBuilder setBlockedTasks(Iterable<ObservableTask> task);

  TaskBuilder addBlockedTask(ObservableTask task);
}
