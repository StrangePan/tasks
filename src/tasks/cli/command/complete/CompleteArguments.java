package tasks.cli.command.complete;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class CompleteArguments extends SimpleArguments {
  private CompleteArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<CompleteArguments> {
    public Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, CompleteArguments::new);
    }
  }
}
