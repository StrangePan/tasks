package tasks.cli.command.info;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class InfoArguments extends SimpleArguments {

  public static CliArguments.CommandRegistration registration(Memoized<TaskStore> taskStore) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.INFO)
        .canonicalName("info")
        .aliases("i")
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new InfoArguments.Parser(taskStore))
        .helpDocumentation(
            "Prints all known information about a particular task, including its "
                + "description, all tasks blocking it, and all tasks it is blocking.");
  }

  private InfoArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<InfoArguments> {
    public Parser(Memoized<TaskStore> taskStore) {
      super(taskStore, InfoArguments::new);
    }
  }
}
