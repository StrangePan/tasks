package tasks.cli.command.reopen;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ReopenArguments extends SimpleArguments {
  protected ReopenArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<ReopenArguments> {
    public Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, ReopenArguments::new);
    }
  }
}
