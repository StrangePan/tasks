package tasks.model;

public interface TaskMutator extends TaskBuilder {

  @Override
  TaskBuilder setCompleted(boolean completed);

  TaskMutator setLabel(String label);

  @Override
  TaskBuilder addBlockingTask(Task task);

  TaskMutator removeBlockingTask(Task task);

  @Override
  TaskBuilder addBlockedTask(Task task);

  TaskMutator removeBlockedTask(Task task);
}
