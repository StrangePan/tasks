package tasks.model;

public interface TaskMutator {

  TaskMutator setLabel(String label);

  TaskMutator setCompleted(boolean completed);

  TaskMutator addBlockingTask(Task task);

  TaskMutator removeBlockingTask(Task task);
}
