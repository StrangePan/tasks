package tasks.cli.command.complete;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class CompleteArguments extends SimpleArguments {
  private CompleteArguments(List<Task> tasks) {
    super(tasks);
  }

  public static CliArguments.CommandRegistration registration(Memoized<TaskStore> taskStore) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.COMPLETE)
        .canonicalName("complete")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new CompleteArguments.Parser(taskStore))
        .helpDocumentation(
            "Mark one or more tasks as complete. This can be undone with the reopen "
                + "command. When a task is completed, other tasks it was blocking may "
                + "become unblocked.");
  }

  public static final class Parser extends SimpleArguments.Parser<CompleteArguments> {
    public Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, CompleteArguments::new);
    }
  }
}
