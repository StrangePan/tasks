package tasks.model;

public interface TaskMutator extends TaskBuilder {

  @Override
  TaskMutator setCompleted(boolean completed);

  TaskMutator setLabel(String label);

  @Override
  TaskMutator setBlockingTasks(Iterable<Task> task);

  @Override
  TaskMutator addBlockingTask(Task task);

  TaskMutator removeBlockingTask(Task task);

  @Override
  TaskMutator setBlockedTasks(Iterable<Task> task);

  @Override
  TaskMutator addBlockedTask(Task task);

  TaskMutator removeBlockedTask(Task task);
}
