package tasks.cli.command.remove;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class RemoveArguments extends SimpleArguments {

  public static CliArguments.CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.REMOVE)
        .canonicalName("remove")
        .aliases("rm")
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new RemoveArguments.Parser(taskParser))
        .helpDocumentation(
            "Completely deletes a task. THIS CANNOT BE UNDONE. It is recommended that "
                + "tasks be marked as completed rather than deleted, or amended if their "
                + "content needs to change.");
  }

  private RemoveArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<RemoveArguments> {
    public Parser(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
      super(taskParser, RemoveArguments::new);
    }
  }
}
