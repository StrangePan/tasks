package tasks.model;

public interface TaskMutator extends TaskBuilder {

  TaskMutator setLabel(String label);

  @Override
  TaskMutator setStatus(Task.Status status);

  @Override
  TaskMutator setBlockingTasks(Iterable<? extends Task> task);

  @Override
  TaskMutator addBlockingTask(Task task);

  TaskMutator removeBlockingTask(Task task);

  @Override
  TaskMutator setBlockedTasks(Iterable<? extends Task> task);

  @Override
  TaskMutator addBlockedTask(Task task);

  TaskMutator removeBlockedTask(Task task);
}
