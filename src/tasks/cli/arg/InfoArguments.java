package tasks.cli.arg;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class InfoArguments extends SimpleArguments {
  private InfoArguments(List<Task> tasks) {
    super(tasks);
  }

  static final class Parser extends SimpleArguments.Parser<InfoArguments> {
    protected Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, InfoArguments::new);
    }
  }
}
