package tasks.cli.command.remove;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class RemoveArguments extends SimpleArguments {
  private RemoveArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<RemoveArguments> {
    public Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, RemoveArguments::new);
    }
  }
}
