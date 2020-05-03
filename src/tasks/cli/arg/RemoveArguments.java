package tasks.cli.arg;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class RemoveArguments extends SimpleArguments {
  private RemoveArguments(List<Task> tasks) {
    super(tasks);
  }

  static final class Parser extends SimpleArguments.Parser<RemoveArguments> {
    protected Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, RemoveArguments::new);
    }
  }
}
