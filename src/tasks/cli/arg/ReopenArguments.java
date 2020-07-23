package tasks.cli.arg;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ReopenArguments extends SimpleArguments {
  protected ReopenArguments(List<Task> tasks) {
    super(tasks);
  }

  static final class Parser extends SimpleArguments.Parser<ReopenArguments> {
    protected Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, ReopenArguments::new);
    }
  }
}
