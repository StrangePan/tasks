package tasks.cli.arg;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class CompleteArguments extends SimpleArguments {
  private CompleteArguments(List<Task> tasks) {
    super(tasks);
  }

  static final class Parser extends SimpleArguments.Parser<CompleteArguments> {
    protected Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, CompleteArguments::new);
    }
  }
}
