package tasks.cli.command.reopen;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ReopenArguments extends SimpleArguments {
  public static CliArguments.CommandRegistration registration(Memoized<TaskStore> taskStore) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.REOPEN)
        .canonicalName("reopen")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new ReopenArguments.Parser(taskStore))
        .helpDocumentation(
            "Reopens one or more completed tasks. This can be undone with the complete "
                + "command.");
  }

  protected ReopenArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<ReopenArguments> {
    public Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, ReopenArguments::new);
    }
  }
}
