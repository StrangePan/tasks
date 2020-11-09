package tasks.model;

public interface TaskMutator extends TaskBuilder {

  @Override
  TaskMutator setCompleted(boolean completed);

  TaskMutator setLabel(String label);

  @Override
  TaskMutator setBlockingTasks(Iterable<ObservableTask> task);

  @Override
  TaskMutator addBlockingTask(ObservableTask task);

  TaskMutator removeBlockingTask(ObservableTask task);

  @Override
  TaskMutator setBlockedTasks(Iterable<ObservableTask> task);

  @Override
  TaskMutator addBlockedTask(ObservableTask task);

  TaskMutator removeBlockedTask(ObservableTask task);
}
